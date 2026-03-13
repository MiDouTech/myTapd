package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 企微群Webhook发送器
 * 通过企微群机器人Webhook推送消息
 */
@Component
public class WecomGroupWebhookSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(WecomGroupWebhookSender.class);

    private final WecomClient wecomClient;

    public WecomGroupWebhookSender(WecomClient wecomClient) {
        this.wecomClient = wecomClient;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WECOM_GROUP;
    }

    @Override
    public void send(Long userId, String title, String content) {
        // NotificationSender统一接口不包含chatId/webhook参数，群推送由业务服务显式调用sendToWebhook
        log.debug("企微群消息发送被忽略（缺少Webhook上下文）: userId={}, title={}", userId, title);
    }

    /**
     * 按Webhook地址发送群通知
     */
    public void sendToWebhook(String webhookUrl, String title, String content) {
        sendToWebhook(webhookUrl, title, content, null);
    }

    /**
     * 按Webhook地址发送群通知，并@提交人
     *
     * @param mentionWecomUserid 需要@的企微用户ID（工单提交人），为null时不@任何人
     */
    public void sendToWebhook(String webhookUrl, String title, String content, String mentionWecomUserid) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("企微群Webhook地址为空，跳过发送: title={}", title);
            return;
        }

        String markdown = buildMarkdown(title, content, mentionWecomUserid);
        wecomClient.sendGroupWebhookMarkdown(webhookUrl, markdown);
        log.info("企微群Webhook推送成功: title={}", title);
    }

    private String buildMarkdown(String title, String content, String mentionWecomUserid) {
        String safeTitle = title == null ? "工单通知" : title;
        String safeContent = content == null ? "" : content;
        StringBuilder markdown = new StringBuilder();
        markdown.append("**").append(safeTitle).append("**\n>");
        markdown.append(safeContent.replace("\n", "\n>"));
        if (mentionWecomUserid != null && !mentionWecomUserid.trim().isEmpty()) {
            markdown.append("\n>提交人：<@").append(mentionWecomUserid.trim()).append(">");
        }
        return markdown.toString();
    }
}
