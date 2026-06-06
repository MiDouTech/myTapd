package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单审批操作记录 PO
 * 对应 ticket_approval_record 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_approval_record")
public class TicketApprovalRecordPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("task_id")
    private Long taskId;

    @TableField("node_key")
    private String nodeKey;

    @TableField("action_type")
    private String actionType;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("remark")
    private String remark;

    @TableField("target_assignee_id")
    private Long targetAssigneeId;

    @TableField("target_assignee_name")
    private String targetAssigneeName;
}
