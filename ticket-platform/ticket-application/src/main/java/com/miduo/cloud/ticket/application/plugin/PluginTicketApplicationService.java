package com.miduo.cloud.ticket.application.plugin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.integration.IntegrationAppCredentialResolver;
import com.miduo.cloud.ticket.application.ticket.TicketAssigneeSyncService;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketBugApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketUrgeApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.enums.TicketAction;
import com.miduo.cloud.ticket.common.enums.TicketSource;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.enums.TicketUploadPurpose;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketCreateInput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketCreateOutput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketDetailOutput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketMinePageInput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketMessageInput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginTicketSummaryOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.ImageUploadOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketBugCustomerInfoInput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCommentInput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCreateInput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketPublicDetailOutput;
import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuUploadService;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po.IntegrationAppPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 插件工单服务
 */
@Slf4j
@Service
public class PluginTicketApplicationService {
    private static final Pattern HTML_IMG_TAG_PATTERN = Pattern.compile("(?is)<img\\b[^>]*>");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern HTML_BR_PATTERN = Pattern.compile("(?is)<br\\s*/?>");
    private static final Pattern HTML_P_END_PATTERN = Pattern.compile("(?is)</p\\s*>");
    private static final Pattern DATA_URL_PATTERN = Pattern.compile("(?is)data:image/[^\\s\"']+");
    private static final Pattern INLINE_DATA_IMAGE_TAG_PATTERN =
            Pattern.compile("(?is)<img\\b[^>]*\\bsrc\\s*=\\s*['\"]data:image/[^'\"]+['\"][^>]*>");
    private static final String IMAGE_ONLY_TITLE_FALLBACK = "图片问题反馈";
    private static final int MAX_PLUGIN_DESCRIPTION_LENGTH = 4000;
    private static final int MAX_PROBLEM_SCREENSHOT_LENGTH = 1000;
    private static final int PLUGIN_URGE_COOLDOWN_MINUTES = 30;
    private static final int PLUGIN_URGE_DAILY_LIMIT = 2;

    private final TicketApplicationService ticketApplicationService;
    private final TicketAssigneeSyncService ticketAssigneeSyncService;
    private final TicketBugApplicationService ticketBugApplicationService;
    private final TicketUrgeApplicationService ticketUrgeApplicationService;
    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final IntegrationAppCredentialResolver credentialResolver;
    private final QiniuUploadService qiniuUploadService;
    private final String publicTicketBaseUrl;

    public PluginTicketApplicationService(TicketApplicationService ticketApplicationService,
                                          TicketAssigneeSyncService ticketAssigneeSyncService,
                                          TicketBugApplicationService ticketBugApplicationService,
                                          TicketUrgeApplicationService ticketUrgeApplicationService,
                                          TicketMapper ticketMapper,
                                          TicketLogMapper ticketLogMapper,
                                          IntegrationAppCredentialResolver credentialResolver,
                                          QiniuUploadService qiniuUploadService,
                                          @Value("${ticket.public-base-url:}") String publicTicketBaseUrl) {
        this.ticketApplicationService = ticketApplicationService;
        this.ticketAssigneeSyncService = ticketAssigneeSyncService;
        this.ticketBugApplicationService = ticketBugApplicationService;
        this.ticketUrgeApplicationService = ticketUrgeApplicationService;
        this.ticketMapper = ticketMapper;
        this.ticketLogMapper = ticketLogMapper;
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
        initPluginBugCustomerInfo(ticketId, claims, input, sanitizedDescription);
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
        return toSummaryOutput(requireOwnedTicket(claims, ticketNo));
    }

    /**
     * 返回插件内可展示的详情；归属校验始终使用 LaunchToken 中的应用和用户。
     */
    public PluginTicketDetailOutput getTicketDetail(PluginLaunchTokenClaims claims, String ticketNo) {
        TicketPO ticket = requireOwnedTicket(claims, ticketNo);
        TicketPublicDetailOutput publicDetail = ticketApplicationService.getPublicTicketDetail(ticket.getTicketNo());
        PluginTicketDetailOutput output = new PluginTicketDetailOutput();
        output.setTicketId(ticket.getId());
        output.setTicketNo(ticket.getTicketNo());
        output.setTitle(ticket.getTitle());
        output.setDescription(ticket.getDescription());
        output.setStatus(ticket.getStatus());
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        output.setStatusLabel(status == null ? ticket.getStatus() : status.getLabel());
        output.setPriority(ticket.getPriority());
        output.setCreateTime(ticket.getCreateTime());
        output.setUpdateTime(ticket.getUpdateTime());
        output.setUrgeCount(ticket.getUrgeCount() == null ? 0 : ticket.getUrgeCount());
        String urgeDisabledReason = resolveUrgeDisabledReason(ticket, status, claims.getUserId());
        output.setCanUrge(urgeDisabledReason == null);
        output.setUrgeDisabledReason(urgeDisabledReason);
        List<PluginTicketDetailOutput.Message> messages = new ArrayList<>();
        if (publicDetail.getComments() != null) {
            publicDetail.getComments().forEach(comment -> {
                PluginTicketDetailOutput.Message message = new PluginTicketDetailOutput.Message();
                message.setId(comment.getId());
                message.setUserName(comment.getUserName());
                message.setContent(comment.getContent());
                message.setType(comment.getType());
                message.setCreateTime(comment.getCreateTime());
                messages.add(message);
            });
        }
        output.setMessages(messages);
        return output;
    }

    /**
     * 将插件用户的补充信息追加到原工单评论时间线。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addMessage(PluginLaunchTokenClaims claims, String ticketNo, PluginTicketMessageInput input) {
        TicketPO ticket = requireOwnedTicket(claims, ticketNo);
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        if (status != null && status.isTerminal()) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "终态工单不可补充信息，请创建关联工单");
        }
        TicketCommentInput commentInput = new TicketCommentInput();
        commentInput.setContent(buildCustomerSupplementHtml(input));
        List<Long> notifyUserIds = ticketAssigneeSyncService.listActiveUserIds(ticket.getId());
        if (notifyUserIds.isEmpty() && ticket.getAssigneeId() != null) {
            notifyUserIds = Collections.singletonList(ticket.getAssigneeId());
        }
        commentInput.setMentionedUserIds(notifyUserIds);
        return ticketApplicationService.addComment(ticket.getId(), commentInput, claims.getUserId());
    }

    /**
     * 插件用户催办自己的工单。
     */
    @Transactional(rollbackFor = Exception.class)
    public void urgeTicket(PluginLaunchTokenClaims claims, String ticketNo) {
        TicketPO ticket = requireOwnedTicket(claims, ticketNo);
        String disabledReason = resolveUrgeDisabledReason(
                ticket, TicketStatus.fromCode(ticket.getStatus()), claims.getUserId());
        if (disabledReason != null) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, disabledReason);
        }
        ticketUrgeApplicationService.urgeByTicketId(ticket.getId(), claims.getUserId(), null);
        TicketCommentInput timelineRecord = new TicketCommentInput();
        timelineRecord.setContent("<p><strong>客户催单：</strong>客户通过工单插件发起催办。</p>");
        ticketApplicationService.addComment(ticket.getId(), timelineRecord, claims.getUserId());
    }

    private TicketPO requireOwnedTicket(PluginLaunchTokenClaims claims, String ticketNo) {
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
        return ticket;
    }

    private String buildCustomerSupplementHtml(PluginTicketMessageInput input) {
        boolean hasContent = StringUtils.hasText(input.getContent());
        boolean hasImages = input.getAttachments() != null && !input.getAttachments().isEmpty();
        boolean hasVideos = input.getVideos() != null && !input.getVideos().isEmpty();
        if (!hasContent && !hasImages && !hasVideos) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "请填写补充内容或上传图片/视频");
        }
        StringBuilder html = new StringBuilder("<p><strong>客户补充：</strong></p>");
        if (hasContent) {
            String escaped = HtmlUtils.htmlEscape(input.getContent().trim()).replace("\n", "<br>");
            html.append("<p>").append(escaped).append("</p>");
        }
        if (input.getAttachments() != null) {
            for (String attachment : input.getAttachments()) {
                if (!StringUtils.hasText(attachment)
                        || attachment.length() > 1000
                        || !(attachment.startsWith("https://") || attachment.startsWith("http://"))) {
                    throw BusinessException.of(ErrorCode.PARAM_ERROR, "补充图片地址不合法");
                }
                html.append("<p><img src=\"")
                        .append(HtmlUtils.htmlEscape(attachment.trim()))
                        .append("\" alt=\"客户补充图片\"></p>");
            }
        }
        if (input.getVideos() != null) {
            for (String video : input.getVideos()) {
                validateSupplementUrl(video, "补充视频地址不合法");
                html.append("<p><video controls preload=\"metadata\" src=\"")
                        .append(HtmlUtils.htmlEscape(video.trim()))
                        .append("\">您的浏览器不支持视频播放</video></p>");
            }
        }
        return html.toString();
    }

    private void validateSupplementUrl(String url, String errorMessage) {
        if (!StringUtils.hasText(url)
                || url.length() > 1000
                || !(url.startsWith("https://") || url.startsWith("http://"))) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, errorMessage);
        }
    }

    private String resolveUrgeDisabledReason(TicketPO ticket, TicketStatus status, Long userId) {
        if (status == null || status.isTerminal()) {
            return "当前工单状态不可催办";
        }
        if (status == TicketStatus.PENDING_ASSIGN || status == TicketStatus.ALERT_TRIGGERED) {
            return "工单尚未分派处理人";
        }
        if (ticket.getAssigneeId() == null) {
            return "工单暂无处理人";
        }
        Date dayStart = startOfToday();
        Long todayCount = ticketLogMapper.selectCount(new LambdaQueryWrapper<TicketLogPO>()
                .eq(TicketLogPO::getTicketId, ticket.getId())
                .eq(TicketLogPO::getUserId, userId)
                .eq(TicketLogPO::getAction, TicketAction.URGE.getCode())
                .ge(TicketLogPO::getCreateTime, dayStart));
        if (todayCount != null && todayCount >= PLUGIN_URGE_DAILY_LIMIT) {
            return "今日催单次数已达上限（每天最多2次）";
        }
        TicketLogPO latest = ticketLogMapper.selectOne(new LambdaQueryWrapper<TicketLogPO>()
                .eq(TicketLogPO::getTicketId, ticket.getId())
                .eq(TicketLogPO::getUserId, userId)
                .eq(TicketLogPO::getAction, TicketAction.URGE.getCode())
                .orderByDesc(TicketLogPO::getCreateTime)
                .last("LIMIT 1"));
        if (latest != null && latest.getCreateTime() != null) {
            long nextAllowedAt = latest.getCreateTime().getTime() + PLUGIN_URGE_COOLDOWN_MINUTES * 60_000L;
            long remainingMillis = nextAllowedAt - System.currentTimeMillis();
            if (remainingMillis > 0) {
                long remainingMinutes = Math.max(1L, (remainingMillis + 59_999L) / 60_000L);
                return "催单过于频繁，请" + remainingMinutes + "分钟后再试";
            }
        }
        return null;
    }

    private Date startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 插件上传图片并返回可访问 URL
     */
    public ImageUploadOutput uploadImage(PluginLaunchTokenClaims claims, MultipartFile file) {
        return uploadFile(claims, file, TicketUploadPurpose.SCREENSHOT);
    }

    public ImageUploadOutput uploadFile(PluginLaunchTokenClaims claims, MultipartFile file,
                                        TicketUploadPurpose purpose) {
        IntegrationAppPO app = credentialResolver.requireEnabledApp(claims.getIntegrationAppId());
        if (app == null) {
            throw BusinessException.of(ErrorCode.PLUGIN_APP_DISABLED, "接入应用已禁用");
        }
        try {
            String fileUrl = qiniuUploadService.uploadForTicket(file, purpose);
            ImageUploadOutput output = new ImageUploadOutput();
            output.setUrl(fileUrl);
            output.setFileName(file.getOriginalFilename());
            output.setFileSize(file.getSize());
            output.setFileType(file.getContentType());
            return output;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("插件上传文件异常: appId={}, userId={}, fileName={}, purpose={}",
                    claims.getIntegrationAppId(), claims.getUserId(),
                    file == null ? null : file.getOriginalFilename(), purpose, ex);
            throw BusinessException.of(ErrorCode.UPLOAD_FAILED, "文件上传失败，请稍后重试");
        }
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

    private void initPluginBugCustomerInfo(Long ticketId,
                                           PluginLaunchTokenClaims claims,
                                           PluginTicketCreateInput input,
                                           String sanitizedDescription) {
        TicketBugCustomerInfoInput bugCustomerInfo = buildBugCustomerInfo(claims, input, sanitizedDescription);
        if (isBugCustomerInfoEmpty(bugCustomerInfo)) {
            return;
        }
        try {
            ticketBugApplicationService.initCustomerInfoFromBot(ticketId, bugCustomerInfo);
        } catch (Exception ex) {
            // 为什么吞掉异常：客服字段回填是增强能力，不能反向影响主链路建单成功。
            log.warn("插件工单客服字段自动回填失败，ticketId={}", ticketId, ex);
        }
    }

    private TicketBugCustomerInfoInput buildBugCustomerInfo(PluginLaunchTokenClaims claims,
                                                            PluginTicketCreateInput input,
                                                            String sanitizedDescription) {
        Map<String, String> customFields = input.getCustomFields() == null ? Collections.emptyMap() : input.getCustomFields();
        Map<String, Object> pluginContext = input.getPluginContext() == null ? Collections.emptyMap() : input.getPluginContext();
        Map<String, Object> extra = asObjectMap(pluginContext.get("extra"));
        Map<String, Object> contextUser = asObjectMap(pluginContext.get("user"));

        String merchantNo = firstNonBlank(
                readString(customFields, "merchantNo", "merchant_no", "memberLogin", "memberlogin", "商户编号"),
                readObjectMapString(pluginContext, "merchantNo", "merchant_no", "memberLogin", "memberlogin", "bizNo", "bizId"),
                readObjectMapString(extra, "merchantNo", "merchant_no", "memberLogin", "memberlogin"),
                readObjectMapString(contextUser, "merchantNo", "memberLogin", "memberlogin", "id", "externalUserId"),
                claims == null ? null : claims.getExternalUserId()
        );

        String companyName = firstNonBlank(
                readString(customFields, "companyName", "company_name", "merchantName", "brandName", "orgName", "公司名称"),
                readObjectMapString(pluginContext, "companyName", "merchantName", "brandName", "orgName"),
                readObjectMapString(extra, "companyName", "merchantName", "brandName", "orgName"),
                readObjectMapString(contextUser, "companyName", "dept", "name")
        );

        String merchantAccount = firstNonBlank(
                readString(customFields, "merchantAccount", "merchant_account", "account", "accountName",
                        "loginName", "userName", "memberLogin", "memberlogin", "商户账号"),
                readObjectMapString(pluginContext, "merchantAccount", "merchant_account", "account", "accountName",
                        "loginName", "userName", "memberLogin", "memberlogin"),
                readObjectMapString(extra, "merchantAccount", "merchant_account", "account", "accountName",
                        "loginName", "userName", "memberLogin", "memberlogin"),
                readObjectMapString(contextUser, "merchantAccount", "account", "accountName", "loginName",
                        "userName", "memberLogin", "memberlogin", "id"),
                merchantNo
        );

        String expectedResult = firstNonBlank(
                readString(customFields, "expectedResult", "expected_result", "预期结果"),
                readObjectMapString(pluginContext, "expectedResult", "expected_result"),
                readObjectMapString(extra, "expectedResult", "expected_result")
        );

        String sceneCode = firstNonBlank(
                readString(customFields, "sceneCode", "scene_code", "场景码"),
                readObjectMapString(pluginContext, "sceneCode", "scene_code", "module", "bizType", "page"),
                readObjectMapString(extra, "sceneCode", "scene_code", "module", "bizType", "page")
        );

        TicketBugCustomerInfoInput output = new TicketBugCustomerInfoInput();
        output.setMerchantNo(merchantNo);
        output.setCompanyName(companyName);
        output.setMerchantAccount(merchantAccount);
        output.setProblemDesc(toBugProblemDesc(sanitizedDescription));
        output.setExpectedResult(expectedResult);
        output.setSceneCode(sceneCode);
        output.setProblemScreenshot(joinProblemScreenshotUrls(input.getAttachments()));
        return output;
    }

    private Map<String, Object> asObjectMap(Object value) {
        if (!(value instanceof Map)) {
            return Collections.emptyMap();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    private String readString(Map<String, String> source, String... keys) {
        if (source == null || source.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = source.get(key);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String readObjectMapString(Map<String, Object> source, String... keys) {
        if (source == null || source.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String toBugProblemDesc(String sanitizedDescription) {
        if (!StringUtils.hasText(sanitizedDescription)) {
            return null;
        }
        String text = HTML_BR_PATTERN.matcher(sanitizedDescription).replaceAll("\n");
        text = HTML_P_END_PATTERN.matcher(text).replaceAll("\n");
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        text = HtmlUtils.htmlUnescape(text);
        text = text.replace("\r", "");
        text = text.replaceAll("[\\t\\x0B\\f]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.replaceAll(" {2,}", " ");
        text = text.trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private String joinProblemScreenshotUrls(List<String> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        StringBuilder joined = new StringBuilder();
        for (String item : attachments) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String url = item.trim();
            if (url.isEmpty()) {
                continue;
            }
            int appendLength = url.length() + (joined.length() > 0 ? 1 : 0);
            if (joined.length() + appendLength > MAX_PROBLEM_SCREENSHOT_LENGTH) {
                break;
            }
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(url);
        }
        return joined.length() == 0 ? null : joined.toString();
    }

    private boolean isBugCustomerInfoEmpty(TicketBugCustomerInfoInput input) {
        if (input == null) {
            return true;
        }
        return !StringUtils.hasText(input.getMerchantNo())
                && !StringUtils.hasText(input.getCompanyName())
                && !StringUtils.hasText(input.getMerchantAccount())
                && !StringUtils.hasText(input.getProblemDesc())
                && !StringUtils.hasText(input.getExpectedResult())
                && !StringUtils.hasText(input.getSceneCode())
                && !StringUtils.hasText(input.getProblemScreenshot());
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
