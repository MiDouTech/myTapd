package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Bug简报状态枚举
 */
@Getter
@AllArgsConstructor
public enum BugReportStatus {

    DRAFT("DRAFT", "待填写"),
    PENDING_REVIEW("PENDING_REVIEW", "待审核"),
    REJECTED("REJECTED", "已退回"),
    ARCHIVED("ARCHIVED", "已归档"),
    VOIDED("VOIDED", "已作废");

    private final String code;
    private final String label;

    public static BugReportStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BugReportStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
