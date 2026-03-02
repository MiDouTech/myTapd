package com.miduo.cloud.ticket.entity.dto.dashboard;

import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘趋势点位
 */
@Data
public class DashboardTrendPointOutput implements Serializable {

    private String day;

    private Long createdCount;

    private Long closedCount;

    private Long backlogCount;
}
