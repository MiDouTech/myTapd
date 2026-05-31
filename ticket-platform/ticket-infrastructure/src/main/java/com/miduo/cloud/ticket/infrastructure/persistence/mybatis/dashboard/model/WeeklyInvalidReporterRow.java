package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model;

import lombok.Data;

/**
 * 无效反馈周报-按反馈人统计行
 */
@Data
public class WeeklyInvalidReporterRow {

    private Long reporterId;

    private String reporterName;

    private Long total;
}
