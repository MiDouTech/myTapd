package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 有效报告选项（用于工单关闭后的人工确认）。
 */
@Getter
@AllArgsConstructor
public enum ValidReportOption {

    YES("YES", "是"),
    NO("NO", "否");

    private final String code;
    private final String label;

    public static ValidReportOption fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        for (ValidReportOption option : values()) {
            if (option.code.equals(normalized)) {
                return option;
            }
        }
        return null;
    }
}

