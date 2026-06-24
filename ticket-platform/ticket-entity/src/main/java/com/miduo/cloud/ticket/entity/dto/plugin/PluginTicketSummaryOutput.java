package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 插件工单摘要
 */
@Data
public class PluginTicketSummaryOutput implements Serializable {

    private Long ticketId;

    private String ticketNo;

    private String title;

    private String status;

    private String statusLabel;

    private String priority;

    private Date createTime;

    private Date updateTime;
}
