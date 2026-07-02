package com.miduo.cloud.ticket.application.plugin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.integration.IntegrationAppCredentialResolver;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.enums.TicketSource;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.enums.TicketUploadPurpose;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketCreateInput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketCreateOutput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketMinePageInput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketSummaryOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.ImageUploadOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCreateInput;
import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuUploadService;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po.IntegrationAppPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 插件工单服务
 */
@Service
public class PluginTicketApplicationService {
    private static final Pattern HTML_IMG_TAG_PATTERN = Pattern.compile("(?is)<img\\b[^>]*>");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern DATA_URL_PATTERN = Pattern.compile("(?is)data:image/[^\\s\"']+");
    private static final Pattern INLINE_DATA_IMAGE_TAG_PATTERN =
            Pattern.compile("(?is)<img\\b[^>]*\\bsrc\\s*=\\s*['\"]data:image/[^'\"]+['\"][^>]*>");
    private static final String IMAGE_ONLY_TITLE_FALLBACK = "图片问题反馈";
    private static final int MAX_PLUGIN_DESCRIPTION_LENGTH = 4000;

    private final TicketApplicationService ticketApplicationService;
    private final TicketMapper ticketMapper;
    private final IntegrationAppCredentialResolver credentialResolver;
    private final QiniuUploadService qiniuUploadService;
    private final String publicTicketBaseUrl;

    public PluginTicketApplicationService(TicketApplicationService ticketApplicationService,
                                          TicketMapper ticketMapper,
                                          IntegrationAppCredentialResolver credentialResolver,
                                          QiniuUploadService qiniuUploadService,
                                          @Value("${ticket.public-base-url:}") String publicTicketBaseUrl) {
        this.ticketApplicationService = ticketApplicationService;
        this.ticketMapper = ticketMapper;
        this.credentialResolver = credentialResolver;
        this.qiniuUploadService = qiniuUploadService;
        this.publicTicketBaseUrl = publicTicketBaseUrl;
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginTicketCreateOutput createTicket(PluginLaunchTokenClaims claims, PluginTicketCreateInput input) {
        IntegrationAppPO app = credentialResolver.requireEnabledApp(claims.getIntegrationAppId());
        if (app == null) {
            throw BusinessException.of(ErrorCode.PLUGIN_APP_DISABLED, "接入应用已禁用");
        }
        if (StringUtils.hasText(input.getExternalTicketRef())) {
            TicketPO existing = ticketMapper.selectByIntegrationRef(
                    claims.getIntegrationAppId(), input.getExternalTicketRef().trim());
            if (existing != null) {
                return toCreateOutput(existing);
            }
        }
        Long categoryId = resolveCategoryId(app, input.getPluginContext());
        String priority = normalizePriority(input.getPriority());
        String sanitizedDescription = sanitizePluginDescription(input.getDescription(), input.getAttachments());
        String title = buildTitle(app.getSystemCode(), input.getPluginContext(), sanitizedDescription);
        Map<String, String> customFields = mergeCustomFields(input.getCustomFields(), input.getPluginContext(), input.getAttachments());

        TicketCreateInput createInput = new TicketCreateInput();
        createInput.setTitle(title);
        createInput.setDescription(sanitizedDescription);
        createInput.setCategoryId(categoryId);
        createInput.setPriority(priority);
        createInput.setSource(TicketSource.PLUGIN.getCode());
        createInput.setIntegrationAppId(claims.getIntegrationAppId());
        createInput.setExternalUserId(claims.getExternalUserId());
        createInput.setExternalTicketRef(trimToNull(input.getExternalTicketRef()));
        if (input.getPluginContext() != null && !input.getPluginContext().isEmpty()) {
            createInput.setPluginContext(JSON.toJSONString(input.getPluginContext()));
        }
        createInput.setCustomFields(customFields);

        Long ticketId = ticketApplicationService.createTicket(createInput, claims.getUserId());
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "工单创建失败");
        }
        return toCreateOutput(ticket);
    }

    public PageOutput<PluginTicketSummaryOutput> listMineTickets(PluginLaunchTokenClaims claims,
                                                                 PluginTicketMinePageInput input) {
        Page<TicketPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        LambdaQueryWrapper<TicketPO> wrapper = new LambdaQueryWrapper<TicketPO>()
                .eq(TicketPO::getIntegrationAppId, claims.getIntegrationAppId())
                .eq(TicketPO::getCreatorId, claims.getUserId())
                .eq(TicketPO::getSource, TicketSource.PLUGIN.getCode())
                .orderByDesc(TicketPO::getCreateTime);
        Page<TicketPO> result = ticketMapper.selectPage(page, wrapper);
        List<PluginTicketSummaryOutput> records = result.getRecords().stream()
                .map(this::toSummaryOutput)
                .collect(Collectors.toList());
        return PageOutput.of(records, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    public PluginTicketSummaryOutput getTicketSummary(PluginLaunchTokenClaims claims, String ticketNo) {
        if (!StringUtils.hasText(ticketNo)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "工单编号不能为空");
        }
        TicketPO ticket = ticketMapper.selectOne(new LambdaQueryWrapper<TicketPO>()
                .eq(TicketPO::getTicketNo, ticketNo.trim())
                .eq(TicketPO::getIntegrationAppId, claims.getIntegrationAppId())
                .eq(TicketPO::getCreatorId, claims.getUserId())
                .last("LIMIT 1"));
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "工单不存在");
        }
        return toSummaryOutput(ticket);
    }

    /**
     * 插件上传图片并返回可访问 URL
     */
    public ImageUploadOutput uploadImage(PluginLaunchTokenClaims claims, MultipartFile file) {
        IntegrationAppPO app = credentialResolver.requireEnabledApp(claims.getIntegrationAppId());
        if (app == null) {
            throw BusinessException.of(ErrorCode.PLUGIN_APP_DISABLED, "接入应用已禁用");
        }

        String fileUrl = qiniuUploadService.uploadForTicket(file, TicketUploadPurpose.SCREENSHOT);
        ImageUploadOutput output = new ImageUploadOutput();
        output.setUrl(fileUrl);
        output.setFileName(file.getOriginalFilename());
        output.setFileSize(file.getSize());
        output.setFileType(file.getContentType());
        return output;
    }

    private Long resolveCategoryId(IntegrationAppPO app, Map<String, Object> pluginContext) {
        if (pluginContext != null && StringUtils.hasText(app.getCategoryMapping())) {
            Object bizType = pluginContext.get("bizType");
            if (bizType != null) {
                Map<String, Long> mapping = JSON.parseObject(app.getCategoryMapping(),
                        new TypeReference<Map<String, Long>>() {
                        });
                if (mapping != null) {
                    Long mapped = mapping.get(String.valueOf(bizType));
                    if (mapped != null) {
                        return mapped;
                    }
                }
            }
        }
        return app.getDefaultCategoryId();
    }

    private String normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return Priority.MEDIUM.getCode();
        }
        String code = priority.trim();
        for (Priority item : Priority.values()) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return item.getCode();
            }
        }
        throw BusinessException.of(ErrorCode.PARAM_ERROR, "优先级不合法");
    }

    private String buildTitle(String systemCode, Map<String, Object> pluginContext, String description) {
        String module = "";
        if (pluginContext != null && pluginContext.get("module") != null) {
            module = String.valueOf(pluginContext.get("module"));
        }
        String prefix = "[" + systemCode + (StringUtils.hasText(module) ? "/" + module : "") + "] ";
        String content = extractTitleText(description);
        if (!StringUtils.hasText(content)) {
            content = IMAGE_ONLY_TITLE_FALLBACK;
        }
        if (content.length() > 50) {
            content = content.substring(0, 50);
        }
        String title = prefix + content;
        return title.length() > 300 ? title.substring(0, 300) : title;
    }

    private String extractTitleText(String description) {
        if (!StringUtils.hasText(description)) {
            return "";
        }
        // 为什么这里先去掉图片标签：富文本里可能带 base64 图片，直接截断会把“图片编码”塞进标题。
        String text = HTML_IMG_TAG_PATTERN.matcher(description).replaceAll(" ");
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        text = HtmlUtils.htmlUnescape(text);
        text = DATA_URL_PATTERN.matcher(text).replaceAll(" ");
        return text.replaceAll("\\s+", " ").trim();
    }

    private String sanitizePluginDescription(String description, List<String> attachments) {
        String raw = StringUtils.hasText(description) ? description.trim() : "";
        if (!StringUtils.hasText(raw)) {
            return "<p>用户反馈了问题，请协助排查。</p>";
        }

        String sanitized = INLINE_DATA_IMAGE_TAG_PATTERN.matcher(raw).replaceAll("");
        sanitized = DATA_URL_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.trim();

        String plainText = extractTitleText(sanitized);
        boolean hasAttachment = attachments != null && !attachments.isEmpty();
        if (!StringUtils.hasText(plainText) && !hasAttachment) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "检测到粘贴图片，请先使用“上传图片”按钮上传后再提交");
        }
        if (!StringUtils.hasText(plainText) && hasAttachment) {
            sanitized = "<p>用户上传了问题图片，请结合附件排查。</p>";
        }
        // 为什么这里限制长度：兼容历史环境中 description 列定义偏小，避免再次触发 Data too long。
        if (sanitized.length() > MAX_PLUGIN_DESCRIPTION_LENGTH) {
            sanitized = sanitized.substring(0, MAX_PLUGIN_DESCRIPTION_LENGTH);
        }
        return sanitized;
    }

    private Map<String, String> mergeCustomFields(Map<String, String> customFields,
                                                    Map<String, Object> pluginContext,
                                                    List<String> attachments) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (customFields != null) {
            merged.putAll(customFields);
        }
        if (pluginContext != null) {
            Object bizId = pluginContext.get("bizId");
            if (bizId != null) {
                merged.put("bizId", String.valueOf(bizId));
            }
            Object pageUrl = pluginContext.get("pageUrl");
            if (pageUrl != null) {
                merged.put("pageUrl", String.valueOf(pageUrl));
            }
        }
        if (attachments != null && !attachments.isEmpty()) {
            merged.put("pluginAttachments", JSON.toJSONString(attachments));
        }
        return merged.isEmpty() ? null : merged;
    }

    private PluginTicketCreateOutput toCreateOutput(TicketPO ticket) {
        PluginTicketCreateOutput output = new PluginTicketCreateOutput();
        output.setTicketId(ticket.getId());
        output.setTicketNo(ticket.getTicketNo());
        output.setStatus(ticket.getStatus());
        output.setPublicUrl(buildPublicUrl(ticket.getTicketNo()));
        return output;
    }

    private PluginTicketSummaryOutput toSummaryOutput(TicketPO ticket) {
        PluginTicketSummaryOutput output = new PluginTicketSummaryOutput();
        output.setTicketId(ticket.getId());
        output.setTicketNo(ticket.getTicketNo());
        output.setTitle(ticket.getTitle());
        output.setStatus(ticket.getStatus());
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        output.setStatusLabel(status != null ? status.getLabel() : ticket.getStatus());
        output.setPriority(ticket.getPriority());
        output.setCreateTime(ticket.getCreateTime());
        output.setUpdateTime(ticket.getUpdateTime());
        return output;
    }

    private String buildPublicUrl(String ticketNo) {
        if (!StringUtils.hasText(publicTicketBaseUrl)) {
            return "/open/ticket/" + ticketNo;
        }
        String base = publicTicketBaseUrl.endsWith("/")
                ? publicTicketBaseUrl.substring(0, publicTicketBaseUrl.length() - 1)
                : publicTicketBaseUrl;
        return base + "/open/ticket/" + ticketNo;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
