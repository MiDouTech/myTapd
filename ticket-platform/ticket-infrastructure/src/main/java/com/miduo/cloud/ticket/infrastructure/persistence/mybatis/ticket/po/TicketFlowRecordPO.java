package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单流转流水PO
 * 记录每一次工单状态流转、转派、退回、分派的完整快照
 * 比 ticket_log 更结构化，专为流转分析和审计设计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_flow_record")
public class TicketFlowRecordPO extends BaseEntity {

    /** 工单ID */
    @TableField("ticket_id")
    private Long ticketId;

    /** 工单编号（冗余，便于直接查询） */
    @TableField("ticket_no")
    private String ticketNo;

    /**
     * 流转类型
     * TRANSIT  - 状态流转（由工作流引擎驱动）
     * TRANSFER - 同角色转派（处理人变更，状态不变）
     * RETURN   - 退回上游节点
     * ASSIGN   - 自动/手动分派
     * CLOSE    - 强制关闭（管理员操作）
     */
    @TableField("flow_type")
    private String flowType;

    /** 触发的流转规则ID（对应工作流 transitions[].id，如 t01） */
    @TableField("transition_id")
    private String transitionId;

    /** 流转动作名称（如：受理、处理完成、验收通过） */
    @TableField("transition_name")
    private String transitionName;

    /** 流转前状态码 */
    @TableField("from_status")
    private String fromStatus;

    /** 流转后状态码 */
    @TableField("to_status")
    private String toStatus;

    /** 流转前处理人ID */
    @TableField("from_assignee_id")
    private Long fromAssigneeId;

    /** 流转后处理人ID */
    @TableField("to_assignee_id")
    private Long toAssigneeId;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作时的角色（SUBMITTER/HANDLER/ADMIN/TICKET_ADMIN） */
    @TableField("operator_role")
    private String operatorRole;

    /** 备注/原因 */
    @TableField("remark")
    private String remark;
}
