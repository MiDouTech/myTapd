package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工单主表PO
 */
@Data
@TableName("ticket")
public class TicketPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
