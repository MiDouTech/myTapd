package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作日志级别枚举
 * PRD §4 日志级别枚举规范
 */
@Getter
@AllArgsConstructor
public enum LogLevelEnum {

    SYSTEM("SYSTEM", "系统级", "#409eff"),
    BUSINESS("BUSINESS", "业务级", "#67c23a"),
    SECURITY("SECURITY", "安全级", "#e6a23c"),
    ERROR("ERROR", "错误级", "#f56c6c");

    private final String code;
    private final String desc;
    private final String color;

    public static LogLevelEnum fromCode(String code) {
        if (code == null) {
            return BUSINESS;
        }
        for (LogLevelEnum level : values()) {
            if (level.code.equalsIgnoreCase(code)) {
                return level;
            }
        }
        return BUSINESS;
    }
}
