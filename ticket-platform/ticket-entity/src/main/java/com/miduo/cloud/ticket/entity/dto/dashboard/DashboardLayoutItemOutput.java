package com.miduo.cloud.ticket.entity.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘布局配置响应DTO（单条）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardLayoutItemOutput {

    /**
     * 行组Key（overview/trend_category/efficiency_workload）
     */
    private String rowGroupKey;

    /**
     * 排列序号
     */
    private Integer sortOrder;

    /**
     * 是否固定不可拖拽
     */
    private Boolean isFixed;
}
