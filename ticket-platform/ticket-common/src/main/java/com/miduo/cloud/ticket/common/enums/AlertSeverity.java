package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 夜莺告警严重级别枚举
 * severity: 1=P1(最严重), 2=P2, 3=P3
 */
@Getter
@AllArgsConstructor
public enum AlertSeverity {

    P1(1, "P1", "urgent"),
    P2(2, "P2", "high"),
    P3(3, "P3", "medium");

    private final int code;
    private final String label;
    private final String defaultPriority;

    public static AlertSeverity fromCode(Integer code) {
        if (code == null) {
            return P3;
        }
        for (AlertSeverity severity : values()) {
            if (severity.code == code) {
                return severity;
            }
        }
        return P3;
    }
}
