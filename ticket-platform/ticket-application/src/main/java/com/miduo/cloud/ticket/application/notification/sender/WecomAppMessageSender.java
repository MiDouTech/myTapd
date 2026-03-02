package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;
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

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.WECOM_APP;
    }

    @Override
    public void send(Long userId, String title, String content) {
        // TODO: Task006中已实现企微API调用，此处集成企微应用消息推送
        log.info("企微应用消息待发送（预留接口）: userId={}, title={}", userId, title);
    }
}
