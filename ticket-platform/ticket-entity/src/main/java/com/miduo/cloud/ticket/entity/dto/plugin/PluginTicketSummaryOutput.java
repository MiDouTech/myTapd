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

    private Long categoryId;

    private String categoryName;

    /**
     * 当前处理人名称；多人处理时使用顿号拼接。
     */
    private String assigneeName;

    private Date createTime;

    private Date updateTime;
}
