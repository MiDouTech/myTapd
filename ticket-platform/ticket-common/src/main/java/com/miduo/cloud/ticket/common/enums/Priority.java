package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优先级枚举
 */
@Getter
@AllArgsConstructor
public enum Priority {

    URGENT("urgent", "紧急", 1),
    HIGH("high", "高", 2),
    MEDIUM("medium", "中", 3),
    LOW("low", "低", 4);

    private final String code;
    private final String label;
    private final int sortOrder;

    public static Priority fromCode(String code) {
        if (code == null) {
            return MEDIUM;
        }
        for (Priority priority : values()) {
            if (priority.code.equals(code)) {
                return priority;
            }
        }
        return MEDIUM;
    }
}
