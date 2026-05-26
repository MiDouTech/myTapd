package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SLA 延迟通知队列状态
 */
@Getter
@AllArgsConstructor
public enum SlaNotificationPendingStatus {

    PENDING("PENDING", "待发送"),
    SENT("SENT", "已发送"),
    FAILED("FAILED", "发送失败");

    private final String code;
    private final String label;
}
