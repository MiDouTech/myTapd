package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷等级枚举
 */
@Getter
@AllArgsConstructor
public enum SeverityLevel {

    P0("P0", "致命"),
    P1("P1", "严重"),
    P2("P2", "一般"),
    P3("P3", "轻微"),
    P4("P4", "建议");

    private final String code;
    private final String label;

    public static SeverityLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SeverityLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}
