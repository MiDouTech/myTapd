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
        sendToWebhookWithMention(webhookUrl, title, content, null);
    }

    /**
     * 按Webhook地址发送群通知，并@指定成员（企微wecomUserid列表）
     *
     * @param webhookUrl            企微群机器人Webhook地址
     * @param title                 消息标题
     * @param content               消息正文
     * @param mentionedWecomUserIds 需要@的企微userId列表，传null或空则不@任何人
     */
    public void sendToWebhookWithMention(String webhookUrl, String title, String content,
                                         java.util.List<String> mentionedWecomUserIds) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("企微群Webhook地址为空，跳过发送: title={}", title);
            return;
        }
        String markdown = buildMarkdownWithMention(title, content, mentionedWecomUserIds);
        wecomClient.sendGroupWebhookMarkdown(webhookUrl, markdown);
        log.info("企微群Webhook推送成功: title={}, mentionCount={}",
                title, mentionedWecomUserIds == null ? 0 : mentionedWecomUserIds.size());
    }

    /**
     * 按Webhook地址发送预格式化内容（Bug简报归档通知专用）
     * 直接发送已组装好的Markdown正文，末尾追加@mention，不再添加额外标题或引用块前缀
     *
     * @param webhookUrl            企微群机器人Webhook地址
     * @param markdownBody          已组装的Markdown正文（含标题行）
     * @param mentionedWecomUserIds 需要@的企微userId列表
     */
    public void sendReportNoticeToWebhook(String webhookUrl, String markdownBody,
                                          java.util.List<String> mentionedWecomUserIds) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("企微群Webhook地址为空，跳过发送Bug简报通知");
            return;
        }
        StringBuilder markdown = new StringBuilder(markdownBody == null ? "" : markdownBody);
        if (mentionedWecomUserIds != null && !mentionedWecomUserIds.isEmpty()) {
            markdown.append("\n");
            for (String wecomUserId : mentionedWecomUserIds) {
                if (wecomUserId != null && !wecomUserId.trim().isEmpty()) {
                    markdown.append("<@").append(wecomUserId.trim()).append("> ");
                }
            }
        }
        wecomClient.sendGroupWebhookMarkdown(webhookUrl, markdown.toString());
        log.info("企微群Bug简报归档通知推送成功: mentionCount={}",
                mentionedWecomUserIds == null ? 0 : mentionedWecomUserIds.size());
    }

    private String buildMarkdownWithMention(String title, String content,
                                            java.util.List<String> mentionedWecomUserIds) {
        String safeTitle = title == null ? "工单通知" : title;
        String safeContent = content == null ? "" : content;
        StringBuilder markdown = new StringBuilder();
        markdown.append("**").append(safeTitle).append("**\n>");
        markdown.append(safeContent.replace("\n", "\n>"));
        if (mentionedWecomUserIds != null && !mentionedWecomUserIds.isEmpty()) {
            markdown.append("\n>");
            for (String wecomUserId : mentionedWecomUserIds) {
                if (wecomUserId != null && !wecomUserId.trim().isEmpty()) {
                    markdown.append("<@").append(wecomUserId.trim()).append("> ");
                }
            }
        }
        return markdown.toString();
    }
}
