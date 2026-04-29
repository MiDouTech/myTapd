package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 企微应用消息发送器
 * 通过企业微信自建应用向用户推送消息
 */
@Component
public class WecomAppMessageSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(WecomAppMessageSender.class);
    private static final int TEXT_CARD_TITLE_MAX_BYTES = 120;
    private static final int TEXT_CARD_DESCRIPTION_MAX_BYTES = 480;
    private static final String DEFAULT_TITLE = "工单通知";
    private static final String DEFAULT_DESCRIPTION = "<div class=\"normal\">无详细内容</div>";
    private static final String TITLE_TRUNCATE_SUFFIX = "...";
    private static final String DESCRIPTION_TRUNCATE_HINT = " ...(内容过长，已截断)";
    private static final String DEFAULT_DETAIL_URL = "https://work.weixin.qq.com";

    private final SysUserMapper sysUserMapper;
    private final WecomClient wecomClient;
    private final WecomProperties wecomProperties;

    public WecomAppMessageSender(SysUserMapper sysUserMapper,
                                 WecomClient wecomClient,
                                 WecomProperties wecomProperties) {
        this.sysUserMapper = sysUserMapper;
        this.wecomClient = wecomClient;
        this.wecomProperties = wecomProperties;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WECOM_APP;
    }

    @Override
    public void send(Long userId, String title, String content) {
        send(userId, title, content, null);
    }

    @Override
    public void send(Long userId, String title, String content, String detailLink) {
        if (userId == null) {
            log.warn("企微应用消息目标用户为空，跳过发送");
            return;
        }

        SysUserPO user = sysUserMapper.selectById(userId);
        if (user == null) {
            log.warn("企微应用消息目标用户不存在，跳过发送: userId={}", userId);
            return;
        }
        if (user.getWecomUserid() == null || user.getWecomUserid().trim().isEmpty()) {
            log.warn("用户未配置企微UserId，跳过企微发送: userId={}", userId);
            return;
        }

        String detailUrl = resolveDetailUrl(detailLink);
        String safeTitle = buildSafeTextCardTitle(title);
        String safeDescription = buildSafeTextCardDescription(content);
        wecomClient.sendTextCardMessage(user.getWecomUserid(), safeTitle, safeDescription, detailUrl, "查看详情");
        log.info("企微应用消息发送成功（textcard）: userId={}, title={}", userId, safeTitle);
    }

    private String resolveDetailUrl(String detailLink) {
        if (detailLink != null && !detailLink.trim().isEmpty()) {
            return detailLink.trim();
        }
        String trustedDomain = buildDefaultDetailUrl();
        if (trustedDomain == null || trustedDomain.trim().isEmpty()) {
            return DEFAULT_DETAIL_URL;
        }
        return trustedDomain;
    }

    private String buildDefaultDetailUrl() {
        String trustedDomain = wecomProperties.getTrustedDomain();
        if (trustedDomain == null || trustedDomain.trim().isEmpty()) {
            return DEFAULT_DETAIL_URL;
        }
        String domain = trustedDomain.trim();
        if (domain.startsWith("http://") || domain.startsWith("https://")) {
            return domain;
        }
        return "https://" + domain;
    }

    private String buildTextCardDescription(String content) {
        List<String> lines = parseContentLines(content);
        return renderDescriptionLines(lines);
    }

    /**
     * 将工单卡片正文按行拆分（跳过空行），与参考图一致每行一条「标签：值」。
     */
    private List<String> parseContentLines(String content) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] raw = content.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String line : raw) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            lines.add(line.trim());
        }
        return lines;
    }

    private String renderDescriptionLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return DEFAULT_DESCRIPTION;
        }
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(wrapDescriptionLine(escapeHtml(line)));
        }
        return builder.toString();
    }

    private String buildSafeTextCardTitle(String title) {
        String normalizedTitle = sanitizeSingleLine(title);
        if (normalizedTitle.isEmpty()) {
            normalizedTitle = DEFAULT_TITLE;
        }
        if (utf8Bytes(normalizedTitle) <= TEXT_CARD_TITLE_MAX_BYTES) {
            return normalizedTitle;
        }
        String truncatedByDescription = truncateTitleDescription(normalizedTitle);
        if (utf8Bytes(truncatedByDescription) <= TEXT_CARD_TITLE_MAX_BYTES) {
            return truncatedByDescription;
        }
        return truncateWithSuffix(normalizedTitle, TEXT_CARD_TITLE_MAX_BYTES, TITLE_TRUNCATE_SUFFIX);
    }

    private String truncateTitleDescription(String title) {
        String[] separators = {" - ", "：", ":"};
        for (String separator : separators) {
            int separatorIndex = title.indexOf(separator);
            if (separatorIndex <= 0 || separatorIndex >= title.length() - separator.length()) {
                continue;
            }
            String prefix = title.substring(0, separatorIndex + separator.length());
            String description = title.substring(separatorIndex + separator.length()).trim();
            int remainBytes = TEXT_CARD_TITLE_MAX_BYTES - utf8Bytes(prefix);
            if (remainBytes <= 0) {
                continue;
            }
            String truncatedDescription = truncateWithSuffix(description, remainBytes, TITLE_TRUNCATE_SUFFIX);
            if (!truncatedDescription.isEmpty()) {
                return prefix + truncatedDescription;
            }
        }
        return truncateWithSuffix(title, TEXT_CARD_TITLE_MAX_BYTES, TITLE_TRUNCATE_SUFFIX);
    }

    private String buildSafeTextCardDescription(String content) {
        String fullDescription = buildTextCardDescription(content);
        if (utf8Bytes(fullDescription) <= TEXT_CARD_DESCRIPTION_MAX_BYTES) {
            return fullDescription;
        }
        String titleTruncated = tryFitDescriptionByTruncatingTitleValueOnly(content);
        if (titleTruncated != null) {
            return titleTruncated;
        }
        return buildTruncatedTextCardDescription(content);
    }

    /**
     * 超长时只缩短「标题：」后面的工单标题，其它行（状态、操作人等）保持完整，避免整块末尾截断。
     */
    private String tryFitDescriptionByTruncatingTitleValueOnly(String content) {
        List<String> lines = parseContentLines(content);
        if (lines.isEmpty()) {
            return null;
        }
        int titleIndex = -1;
        String titlePrefix = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("标题：")) {
                titleIndex = i;
                titlePrefix = "标题：";
                break;
            }
            if (line.startsWith("标题:")) {
                titleIndex = i;
                titlePrefix = "标题:";
                break;
            }
        }
        if (titleIndex < 0) {
            return null;
        }
        String titleLine = lines.get(titleIndex);
        String titleValue = titleLine.substring(titlePrefix.length());
        List<String> mutable = new ArrayList<>(lines);
        for (int end = titleValue.length(); end >= 0; end--) {
            String newTitleLine = titlePrefix + titleValue.substring(0, end)
                    + (end < titleValue.length() ? DESCRIPTION_TRUNCATE_HINT : "");
            mutable.set(titleIndex, newTitleLine);
            String candidate = renderDescriptionLines(mutable);
            if (utf8Bytes(candidate) <= TEXT_CARD_DESCRIPTION_MAX_BYTES) {
                return candidate;
            }
        }
        return null;
    }

    private String buildTruncatedTextCardDescription(String content) {
        String normalized = sanitizeSingleLine(content);
        if (normalized.isEmpty()) {
            return wrapDescriptionLine("内容过长，已截断");
        }
        String escaped = escapeHtml(normalized);
        String wrappedHint = wrapDescriptionLine("内容过长，已截断");
        String candidate = wrapDescriptionLine(escaped + DESCRIPTION_TRUNCATE_HINT);
        if (utf8Bytes(candidate) <= TEXT_CARD_DESCRIPTION_MAX_BYTES) {
            return candidate;
        }

        int shellBytes = utf8Bytes(wrapDescriptionLine(""));
        int availableBytes = TEXT_CARD_DESCRIPTION_MAX_BYTES - shellBytes - utf8Bytes(DESCRIPTION_TRUNCATE_HINT);
        if (availableBytes <= 0) {
            return wrappedHint;
        }
        String truncated = truncateUtf8(escaped, availableBytes);
        if (truncated.isEmpty()) {
            return wrappedHint;
        }
        String truncatedCandidate = wrapDescriptionLine(truncated + DESCRIPTION_TRUNCATE_HINT);
        if (utf8Bytes(truncatedCandidate) <= TEXT_CARD_DESCRIPTION_MAX_BYTES) {
            return truncatedCandidate;
        }
        return wrappedHint;
    }

    private String wrapDescriptionLine(String content) {
        String value = content == null ? "" : content;
        return "<div class=\"normal\">" + value + "</div>";
    }

    private String truncateWithSuffix(String value, int maxBytes, String suffix) {
        if (value == null || value.isEmpty() || maxBytes <= 0) {
            return "";
        }
        if (utf8Bytes(value) <= maxBytes) {
            return value;
        }
        String safeSuffix = suffix == null ? "" : suffix;
        int suffixBytes = utf8Bytes(safeSuffix);
        if (suffixBytes >= maxBytes) {
            return truncateUtf8(value, maxBytes);
        }
        String truncated = truncateUtf8(value, maxBytes - suffixBytes);
        if (truncated.isEmpty()) {
            return truncateUtf8(value, maxBytes);
        }
        return truncated + safeSuffix;
    }

    private String sanitizeSingleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\r", " ").replace("\n", " ");
    }

    private String truncateUtf8(String value, int maxBytes) {
        if (value == null || value.isEmpty() || maxBytes <= 0) {
            return "";
        }
        int bytes = 0;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            String ch = String.valueOf(value.charAt(i));
            int chBytes = utf8Bytes(ch);
            if (bytes + chBytes > maxBytes) {
                break;
            }
            builder.append(ch);
            bytes += chBytes;
        }
        return builder.toString();
    }

    private int utf8Bytes(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
