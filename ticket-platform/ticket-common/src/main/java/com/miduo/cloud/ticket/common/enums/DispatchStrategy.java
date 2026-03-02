package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分派策略枚举
 */
@Getter
@AllArgsConstructor
public enum DispatchStrategy {

    MANUAL("MANUAL", "手动分派"),
    CATEGORY_DEFAULT("CATEGORY_DEFAULT", "分类默认分派"),
    ROUND_ROBIN("ROUND_ROBIN", "轮询分派"),
    LOAD_BALANCE("LOAD_BALANCE", "负载均衡分派"),
    MATRIX("MATRIX", "矩阵分派");

    private final String code;
    private final String label;

    public static DispatchStrategy fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DispatchStrategy strategy : values()) {
            if (strategy.code.equals(code)) {
                return strategy;
            }
        }
        return null;
    }
}
