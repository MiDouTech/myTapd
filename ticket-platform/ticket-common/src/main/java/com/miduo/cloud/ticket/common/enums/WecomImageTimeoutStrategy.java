package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 企微图片超时处理策略枚举
 */
@Getter
@AllArgsConstructor
public enum WecomImageTimeoutStrategy {

    CREATE_TICKET("CREATE_TICKET", "静默创建仅含图片的工单"),
    EXPIRE("EXPIRE", "标记过期，不创建工单"),
    NOTIFY_USER("NOTIFY_USER", "通知用户补充文字描述");

    private final String code;
    private final String label;

    public static WecomImageTimeoutStrategy fromCode(String code) {
        if (code == null) {
            return CREATE_TICKET;
        }
        for (WecomImageTimeoutStrategy strategy : values()) {
            if (strategy.code.equalsIgnoreCase(code)) {
                return strategy;
            }
        }
        return CREATE_TICKET;
    }
}
