package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警规则名称匹配模式枚举
 */
@Getter
@AllArgsConstructor
public enum AlertMatchMode {

    EXACT("EXACT", "精确匹配"),
    PREFIX("PREFIX", "前缀匹配");

    private final String code;
    private final String label;

    public static AlertMatchMode fromCode(String code) {
        if (code == null) {
            return EXACT;
        }
        for (AlertMatchMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return EXACT;
    }
}
