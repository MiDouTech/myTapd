package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工作流模式枚举
 */
@Getter
@AllArgsConstructor
public enum WorkflowMode {

    SIMPLE("SIMPLE", "简单模式"),
    ADVANCED("ADVANCED", "高级模式");

    private final String code;
    private final String label;

    public static WorkflowMode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WorkflowMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        return null;
    }
}
