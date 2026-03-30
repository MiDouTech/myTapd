package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷工单复现环境（与 ticket_bug_test_info.reproduce_env 存储值一致）
 */
@Getter
@AllArgsConstructor
public enum BugReproduceEnv {

    PRODUCTION("PRODUCTION", "生产环境"),
    TEST("TEST", "测试环境"),
    BOTH("BOTH", "均可复现");

    private final String code;
    private final String label;

    public static BugReproduceEnv fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String normalized = code.trim().toUpperCase();
        for (BugReproduceEnv v : values()) {
            if (v.code.equals(normalized)) {
                return v;
            }
        }
        return null;
    }
}
