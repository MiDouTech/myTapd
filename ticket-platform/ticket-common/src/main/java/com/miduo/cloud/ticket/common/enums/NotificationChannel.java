package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知渠道枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationChannel {

    SITE("SITE", "站内信"),
    WECOM_APP("WECOM_APP", "企微应用消息"),
    WECOM_GROUP("WECOM_GROUP", "企微群"),
    EMAIL("EMAIL", "邮件");

    private final String code;
    private final String label;

    public static NotificationChannel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotificationChannel channel : values()) {
            if (channel.code.equals(code)) {
                return channel;
            }
        }
        return null;
    }
}
