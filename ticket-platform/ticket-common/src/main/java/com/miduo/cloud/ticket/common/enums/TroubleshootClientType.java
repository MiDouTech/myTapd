package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷工单-接口排障：客户端类型
 */
@Getter
@AllArgsConstructor
public enum TroubleshootClientType {

    H5("H5", "H5"),
    MINI_APP("MINI_APP", "小程序"),
    APP("APP", "App"),
    PC("PC", "PC"),
    UNKNOWN("UNKNOWN", "未知");

    private final String code;
    private final String label;

    public static TroubleshootClientType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String n = code.trim().toUpperCase();
        for (TroubleshootClientType t : values()) {
            if (t.code.equals(n)) {
                return t;
            }
        }
        return null;
    }
}
