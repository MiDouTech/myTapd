package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SLA计时器类型枚举
 */
@Getter
@AllArgsConstructor
public enum SlaTimerType {

    RESPONSE("RESPONSE", "首次响应"),
    RESOLVE("RESOLVE", "解决");

    private final String code;
    private final String label;

    public static SlaTimerType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SlaTimerType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
