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
    TICKET_ASSIGNED("TICKET_ASSIGNED", "工单分派"),
    TICKET_COMPLETED("TICKET_COMPLETED", "工单完结"),
    TICKET_CLOSED("TICKET_CLOSED", "工单关闭"),
    /** 工单评论中 @ 指定用户（企微机器人推送时对被 @ 人执行 mentioned_list） */
    TICKET_COMMENT_MENTION("TICKET_COMMENT_MENTION", "评论@提醒"),
    /**
     * Bug 简报审核通过/直接归档后推送；与工单 Webhook 共用配置 URL 时，
     * 需在 {@code event_types} 中显式订阅本事件（不会自动随工单事件订阅）。
     */
    BUG_REPORT_ARCHIVED("BUG_REPORT_ARCHIVED", "Bug简报归档");

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
