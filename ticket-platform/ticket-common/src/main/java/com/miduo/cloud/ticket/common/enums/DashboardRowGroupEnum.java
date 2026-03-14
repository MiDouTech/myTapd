package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 仪表盘行组Key枚举
 * 定义所有合法的仪表盘行组标识
 */
@Getter
@AllArgsConstructor
public enum DashboardRowGroupEnum {

    OVERVIEW("overview", 0, true),
    TREND_CATEGORY("trend_category", 1, false),
    EFFICIENCY_WORKLOAD("efficiency_workload", 2, false);

    private final String key;
    private final int defaultSortOrder;
    private final boolean isFixed;

    /**
     * 根据字符串Key查找枚举
     *
     * @param key 行组Key字符串
     * @return 对应的枚举值
     * @throws IllegalArgumentException 当key不合法时抛出
     */
    public static DashboardRowGroupEnum fromKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("行组Key不能为空");
        }
        for (DashboardRowGroupEnum value : values()) {
            if (value.key.equals(key)) {
                return value;
            }
        }
        throw new IllegalArgumentException("非法的行组Key: " + key);
    }
}
