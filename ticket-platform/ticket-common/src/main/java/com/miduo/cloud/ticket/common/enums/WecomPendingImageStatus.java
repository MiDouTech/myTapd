package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 企微图片暂存状态枚举
 */
@Getter
@AllArgsConstructor
public enum WecomPendingImageStatus {

    PENDING("PENDING", "待关联"),
    LINKED("LINKED", "已关联"),
    EXPIRED("EXPIRED", "已过期"),
    FAILED("FAILED", "下载失败");

    private final String code;
    private final String label;

    public static WecomPendingImageStatus fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (WecomPendingImageStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
