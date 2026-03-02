package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 看板卡片数据
 */
@Data
public class KanbanTicketOutput implements Serializable {

    private Long id;

    private String ticketNo;

    private String title;

    private String priority;

    private String priorityLabel;

    private String status;

    private String statusLabel;

    private String categoryName;

    private Long assigneeId;

    private String assigneeName;

    private Date updateTime;
}
