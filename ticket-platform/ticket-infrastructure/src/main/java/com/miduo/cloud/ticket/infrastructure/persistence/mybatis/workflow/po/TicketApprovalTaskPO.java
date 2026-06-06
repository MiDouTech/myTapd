package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 工单审批任务 PO
 * 对应 ticket_approval_task 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_approval_task")
public class TicketApprovalTaskPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("transition_id")
    private String transitionId;

    @TableField("node_key")
    private String nodeKey;

    @TableField("node_name")
    private String nodeName;

    @TableField("approve_mode")
    private String approveMode;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("assignee_name")
    private String assigneeName;

    @TableField("task_status")
    private String taskStatus;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("due_time")
    private Date dueTime;

    @TableField("remark")
    private String remark;

    @TableField("operate_time")
    private Date operateTime;
}
