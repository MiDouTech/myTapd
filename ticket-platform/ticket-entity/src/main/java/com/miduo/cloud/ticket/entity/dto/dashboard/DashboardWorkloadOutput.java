package com.miduo.cloud.ticket.entity.dto.dashboard;

import lombok.Data;

import java.io.Serializable;

/**
 * 人员工作量统计
 */
@Data
public class DashboardWorkloadOutput implements Serializable {

    private Long assigneeId;

    private String assigneeName;

    private Long totalCount;

    private Long processingCount;

    private Long completedCount;
}
