package com.miduo.cloud.ticket.entity.dto.dashboard;

import lombok.Data;

import java.io.Serializable;

/**
 * 仪表盘概览数据
 */
@Data
public class DashboardOverviewOutput implements Serializable {

    private Long pendingAcceptCount;

    private Long processingCount;

    private Long suspendedCount;

    private Long completedCount;

    private Long slaBreachedCount;

    private Long totalCount;
}
