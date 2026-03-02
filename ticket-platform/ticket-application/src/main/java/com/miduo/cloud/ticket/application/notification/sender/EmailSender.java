package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 邮件发送器
 * 通过SMTP发送邮件通知
 */
@Component
public class EmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Long userId, String title, String content) {
        // TODO: 集成SMTP邮件发送，需配置邮件服务器信息
        log.info("邮件通知待发送（预留接口）: userId={}, title={}", userId, title);
    }
}
