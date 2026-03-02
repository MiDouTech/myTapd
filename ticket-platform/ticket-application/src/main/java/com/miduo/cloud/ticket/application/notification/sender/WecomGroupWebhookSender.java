package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;
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

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WECOM_GROUP;
    }

    @Override
    public void send(Long userId, String title, String content) {
        // TODO: Task006中已实现企微群Webhook调用，此处集成群消息推送
        log.info("企微群消息待发送（预留接口）: title={}", title);
    }
}
