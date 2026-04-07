package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationType {

    TICKET_CREATED("TICKET_CREATED", "工单创建"),
    STATUS_CHANGED("STATUS_CHANGED", "状态变更"),
    ASSIGNED("ASSIGNED", "工单分派"),
    SLA_WARNING("SLA_WARNING", "SLA预警"),
    SLA_BREACHED("SLA_BREACHED", "SLA超时"),
    COMMENT("COMMENT", "工单评论"),
    /** 工单评论中 @ 指定用户（与普评区分，且不参与通知合并） */
    COMMENT_MENTION("COMMENT_MENTION", "评论@提醒"),
    URGE("URGE", "催办"),
    REPORT_SUBMITTED("REPORT_SUBMITTED", "简报提审"),
    REPORT_APPROVED("REPORT_APPROVED", "简报审核通过"),
    REPORT_REJECTED("REPORT_REJECTED", "简报审核驳回"),
    REPORT_REMIND("REPORT_REMIND", "简报提醒");

    private final String code;
    private final String label;

    public static NotificationType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotificationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
