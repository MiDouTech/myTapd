package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 企微应用消息发送器
 * 通过企业微信自建应用向用户推送消息
 */
@Component
public class WecomAppMessageSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(WecomAppMessageSender.class);

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

        String detailUrl = buildDefaultDetailUrl();
        String description = buildTextCardDescription(content);
        wecomClient.sendTextCardMessage(user.getWecomUserid(), title, description, detailUrl, "查看详情");
        log.info("企微应用消息发送成功: userId={}, title={}", userId, title);
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

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
