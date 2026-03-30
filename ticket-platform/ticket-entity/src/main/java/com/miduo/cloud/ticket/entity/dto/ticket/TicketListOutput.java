package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TicketListOutput implements Serializable {

    private Long id;

    private String ticketNo;

    private String title;

    /**
     * 公司名称（缺陷工单客服信息，无则 null）
     */
    private String companyName;

    private Long categoryId;

    private String categoryName;

    private String priority;

    private String priorityLabel;

    private String status;

    private String statusLabel;

    private Long creatorId;

    private String creatorName;

    private Long assigneeId;

    private String assigneeName;

    private String source;

    private String sourceLabel;

    private Date expectedTime;

    private Date createTime;

    private Date updateTime;

    private Date resolvedAt;

    private Date closedAt;

    /**
     * SLA整体状态：NORMAL-正常 / WARNING-预警中 / BREACHED-已超时 / null-无SLA策略
     */
    private String slaStatus;

    /**
     * SLA状态中文标签
     */
    private String slaStatusLabel;
}
