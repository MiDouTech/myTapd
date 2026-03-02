package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单来源枚举
 */
@Getter
@AllArgsConstructor
public enum TicketSource {

    WEB("web", "Web端"),
    WECOM_BOT("wecom_bot", "企微群机器人"),
    API("api", "API接口");

    private final String code;
    private final String label;

    public static TicketSource fromCode(String code) {
        if (code == null) {
            return WEB;
        }
        for (TicketSource source : values()) {
            if (source.code.equalsIgnoreCase(code)) {
                return source;
            }
        }
        return WEB;
    }
}
