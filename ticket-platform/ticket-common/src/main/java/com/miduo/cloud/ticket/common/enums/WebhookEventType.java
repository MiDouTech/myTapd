package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Webhook事件类型枚举
 */
@Getter
@AllArgsConstructor
public enum WebhookEventType {

    TICKET_CREATED("TICKET_CREATED", "工单创建"),
    TICKET_STATUS_CHANGED("TICKET_STATUS_CHANGED", "工单状态变更"),
    TICKET_ASSIGNED("TICKET_ASSIGNED", "工单分派");

    private final String code;
    private final String label;

    public static WebhookEventType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WebhookEventType eventType : values()) {
            if (eventType.code.equals(code)) {
                return eventType;
            }
        }
        return null;
    }
}
