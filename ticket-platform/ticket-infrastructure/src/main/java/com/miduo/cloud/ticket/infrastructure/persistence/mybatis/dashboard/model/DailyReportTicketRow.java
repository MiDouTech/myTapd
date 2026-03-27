package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model;

import lombok.Data;

import java.util.Date;

/**
 * 日报工单明细行
 */
@Data
public class DailyReportTicketRow {

    private Long id;

    private String ticketNo;

    private String title;

    private String status;

    private String statusLabel;

    private String priority;

    private String assigneeName;

    private String categoryName;

    private String severityLevel;

    private Date createTime;

    private Date updateTime;
}
