package com.miduo.cloud.ticket.entity.dto.notification;

import lombok.Data;

import java.io.Serializable;

/**
 * 通知偏好输出DTO
 */
@Data
public class NotificationPreferenceOutput implements Serializable {

    private Long id;

    private Long userId;

    private String eventType;

    private String eventTypeLabel;

    private Integer siteEnabled;

    private Integer wecomEnabled;

    private Integer emailEnabled;
}
