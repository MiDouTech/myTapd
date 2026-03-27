package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model;

import lombok.Data;

/**
 * 日报状态统计行
 */
@Data
public class DailyReportStatusRow {

    private String status;

    private Long total;
}
