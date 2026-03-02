package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * 通知发送事件（用于WebSocket实时推送）
 */
@Getter
public class NotificationSendEvent extends DomainEvent {

    private final Long userId;
    private final Long notificationId;
    private final String type;
    private final String title;
    private final String content;

    public NotificationSendEvent(Long userId, Long notificationId, String type,
                                 String title, String content) {
        super();
        this.userId = userId;
        this.notificationId = notificationId;
        this.type = type;
        this.title = title;
        this.content = content;
    }
}
