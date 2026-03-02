package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket")
public class TicketPO extends BaseEntity {

    @TableField("ticket_no")
    private String ticketNo;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("category_id")
    private Long categoryId;

    @TableField("template_id")
    private Long templateId;

    @TableField("workflow_id")
    private Long workflowId;

    @TableField("priority")
    private String priority;

    @TableField("status")
    private String status;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("source")
    private String source;

    @TableField("source_chat_id")
    private String sourceChatId;

    @TableField("custom_fields")
    private String customFields;

    @TableField("expected_time")
    private Date expectedTime;

    @TableField("resolved_at")
    private Date resolvedAt;

    @TableField("closed_at")
    private Date closedAt;

    @Version
    @TableField("version")
    private Integer version;
}
