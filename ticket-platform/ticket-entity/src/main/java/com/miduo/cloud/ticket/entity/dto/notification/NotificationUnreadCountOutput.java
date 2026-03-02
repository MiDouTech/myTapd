package com.miduo.cloud.ticket.entity.dto.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 未读通知数量输出DTO
 */
@Data
public class NotificationUnreadCountOutput implements Serializable {

    private int unreadCount;

    public NotificationUnreadCountOutput() {
    }

    public NotificationUnreadCountOutput(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
