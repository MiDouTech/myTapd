package com.miduo.cloud.ticket.entity.dto.dashboard;

import lombok.Data;

import java.io.Serializable;

/**
 * 处理效率统计
 */
@Data
public class DashboardEfficiencyOutput implements Serializable {

    private Double avgResponseMinutes;

    private Double avgResolveMinutes;

    private Long completedCount;

    private Long totalCount;

    private Double completionRate;
}
