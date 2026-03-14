package com.miduo.cloud.ticket.common.constants;

import com.miduo.cloud.ticket.common.enums.DashboardRowGroupEnum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 仪表盘布局相关常量
 */
public final class DashboardLayoutConstants {

    private DashboardLayoutConstants() {
    }

    /**
     * Redis Key 前缀：仪表盘个人布局缓存，完整Key格式：dashboard:layout:user:{userId}
     */
    public static final String LAYOUT_CACHE_KEY_PREFIX = "dashboard:layout:user:";

    /**
     * 布局缓存TTL（分钟）
     */
    public static final long LAYOUT_CACHE_TTL_MINUTES = 30L;

    /**
     * 系统默认布局列表（用于用户无个人配置时的 fallback）
     * 顺序和属性与 DashboardRowGroupEnum 定义保持一致
     */
    public static final List<DashboardRowGroupEntry> DEFAULT_LAYOUT;

    static {
        DEFAULT_LAYOUT = Collections.unmodifiableList(Arrays.asList(
                new DashboardRowGroupEntry(DashboardRowGroupEnum.OVERVIEW.getKey(), DashboardRowGroupEnum.OVERVIEW.getDefaultSortOrder(), DashboardRowGroupEnum.OVERVIEW.isFixed()),
                new DashboardRowGroupEntry(DashboardRowGroupEnum.TREND_CATEGORY.getKey(), DashboardRowGroupEnum.TREND_CATEGORY.getDefaultSortOrder(), DashboardRowGroupEnum.TREND_CATEGORY.isFixed()),
                new DashboardRowGroupEntry(DashboardRowGroupEnum.EFFICIENCY_WORKLOAD.getKey(), DashboardRowGroupEnum.EFFICIENCY_WORKLOAD.getDefaultSortOrder(), DashboardRowGroupEnum.EFFICIENCY_WORKLOAD.isFixed())
        ));
    }

    /**
     * 默认布局条目（轻量结构，避免循环依赖）
     */
    public static class DashboardRowGroupEntry {
        private final String rowGroupKey;
        private final int sortOrder;
        private final boolean isFixed;

        public DashboardRowGroupEntry(String rowGroupKey, int sortOrder, boolean isFixed) {
            this.rowGroupKey = rowGroupKey;
            this.sortOrder = sortOrder;
            this.isFixed = isFixed;
        }

        public String getRowGroupKey() {
            return rowGroupKey;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        public boolean isFixed() {
            return isFixed;
        }
    }
}
