package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model;

import lombok.Data;

import java.util.Date;

/**
 * 无效反馈周报-问题明细行
 */
@Data
public class WeeklyInvalidTicketRow {

    private Long id;

    private String ticketNo;

    private String title;

    private Long reporterId;

    private String reporterName;

    private Date closedTime;
}
