package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 所属应用编码枚举
 * PRD §5 所属应用枚举规范
 */
@Getter
@AllArgsConstructor
public enum AppCodeEnum {

    MIDUO_BASE("MIDUO_BASE", "米多球基础"),
    MIDUO_BACKEND("MIDUO_BACKEND", "米多后台"),
    MIDUO_CRM("MIDUO_CRM", "米多球CRM"),
    TICKET_SYSTEM("TICKET_SYSTEM", "工单系统");

    private final String code;
    private final String appName;

    public static AppCodeEnum fromCode(String code) {
        if (code == null) {
            return TICKET_SYSTEM;
        }
        for (AppCodeEnum appCode : values()) {
            if (appCode.code.equalsIgnoreCase(code)) {
                return appCode;
            }
        }
        return TICKET_SYSTEM;
    }
}
