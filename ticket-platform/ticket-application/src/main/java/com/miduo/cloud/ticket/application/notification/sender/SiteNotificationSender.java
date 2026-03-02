package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;
import com.miduo.cloud.ticket.domain.common.event.NotificationSendEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper.NotificationMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po.NotificationPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 站内通知发送器
 * 将通知写入数据库并通过WebSocket实时推送
 */
@Component
public class SiteNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SiteNotificationSender.class);

    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    public SiteNotificationSender(NotificationMapper notificationMapper,
                                  ApplicationEventPublisher eventPublisher) {
        this.notificationMapper = notificationMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SITE;
    }

    @Override
    public void send(Long userId, String title, String content) {
        NotificationPO po = new NotificationPO();
        po.setUserId(userId);
        po.setChannel(NotificationChannel.SITE.getCode());
        po.setTitle(title);
        po.setContent(content);
        po.setIsRead(0);
        notificationMapper.insert(po);

        eventPublisher.publishEvent(new NotificationSendEvent(
                userId, po.getId(), po.getType(), title, content));

        log.info("站内通知已发送: userId={}, title={}", userId, title);
    }

    /**
     * 发送站内通知（带类型和工单关联）
     */
    public void send(Long userId, Long ticketId, Long reportId,
                     String type, String title, String content) {
        NotificationPO po = new NotificationPO();
        po.setUserId(userId);
        po.setTicketId(ticketId);
        po.setReportId(reportId);
        po.setType(type);
        po.setChannel(NotificationChannel.SITE.getCode());
        po.setTitle(title);
        po.setContent(content);
        po.setIsRead(0);
        notificationMapper.insert(po);

        eventPublisher.publishEvent(new NotificationSendEvent(
                userId, po.getId(), type, title, content));

        log.info("站内通知已发送: userId={}, type={}, title={}", userId, type, title);
    }
}
