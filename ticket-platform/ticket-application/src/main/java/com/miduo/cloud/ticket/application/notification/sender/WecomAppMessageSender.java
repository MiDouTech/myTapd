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

/**
 * 企微应用消息发送器
 * 通过企业微信自建应用向用户推送消息
 */
@Component
public class WecomAppMessageSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(WecomAppMessageSender.class);
    private static final int TEXT_CARD_TITLE_MAX_BYTES = 120;
    private static final int TEXT_CARD_DESCRIPTION_MAX_BYTES = 480;
    private static final int TEXT_MESSAGE_MAX_BYTES = 1800;

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

        String detailUrl = (detailLink != null && !detailLink.trim().isEmpty())
                ? detailLink.trim()
                : buildDefaultDetailUrl();
        String textCardDescription = buildTextCardDescription(content);
        boolean canSendTextCard = isTextCardPayloadSafe(title, textCardDescription)
                && detailUrl != null && !detailUrl.trim().isEmpty();
        if (canSendTextCard) {
            wecomClient.sendTextCardMessage(user.getWecomUserid(), title, textCardDescription, detailUrl, "查看详情");
            log.info("企微应用消息发送成功: userId={}, title={}", userId, title);
            return;
        }

        // 为什么需要 text 兜底：textcard 依赖详情链接且长度限制更严格，简报类通知在未配置详情地址时会被企微拒收。
        String textMessage = buildTextMessageContent(title, content);
        wecomClient.sendTextMessage(user.getWecomUserid(), textMessage);
        log.info("企微应用消息发送成功（text兜底）: userId={}, title={}", userId, title);
    }

    private String buildDefaultDetailUrl() {
        String trustedDomain = wecomProperties.getTrustedDomain();
        if (trustedDomain == null || trustedDomain.trim().isEmpty()) {
            return "";
        }
        String domain = trustedDomain.trim();
        if (domain.startsWith("http://") || domain.startsWith("https://")) {
            return domain;
        }
        return "https://" + domain;
    }

    private String buildTextCardDescription(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "<div class=\"normal\">无详细内容</div>";
        }
        String[] lines = content.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            builder.append("<div class=\"normal\">")
                    .append(escapeHtml(line.trim()))
                    .append("</div>");
        }
        if (builder.length() == 0) {
            return "<div class=\"normal\">无详细内容</div>";
        }
        return builder.toString();
    }

    private boolean isTextCardPayloadSafe(String title, String description) {
        return utf8Bytes(title) <= TEXT_CARD_TITLE_MAX_BYTES
                && utf8Bytes(description) <= TEXT_CARD_DESCRIPTION_MAX_BYTES;
    }

    private String buildTextMessageContent(String title, String content) {
        String normalizedTitle = sanitizeSingleLine(title);
        String normalizedContent = sanitizeMultiline(content);
        StringBuilder builder = new StringBuilder();
        if (!normalizedTitle.isEmpty()) {
            builder.append(normalizedTitle);
        } else {
            builder.append("工单通知");
        }
        if (!normalizedContent.isEmpty()) {
            builder.append('\n').append(normalizedContent);
        }
        String result = builder.toString();
        if (utf8Bytes(result) <= TEXT_MESSAGE_MAX_BYTES) {
            return result;
        }
        String suffix = "\n...(内容过长，请到系统查看详情)";
        int maxContentBytes = TEXT_MESSAGE_MAX_BYTES - utf8Bytes(suffix);
        if (maxContentBytes <= 0) {
            return "工单通知";
        }
        return truncateUtf8(result, maxContentBytes) + suffix;
    }

    private String sanitizeSingleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\r", " ").replace("\n", " ");
    }

    private String sanitizeMultiline(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\r", "");
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
