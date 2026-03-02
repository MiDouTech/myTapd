package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SLA计时器状态枚举
 */
@Getter
@AllArgsConstructor
public enum SlaTimerStatus {

    RUNNING("RUNNING", "运行中"),
    PAUSED("PAUSED", "已暂停"),
    COMPLETED("COMPLETED", "已完成"),
    BREACHED("BREACHED", "已超时");

    private final String code;
    private final String label;

    public static SlaTimerStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SlaTimerStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
