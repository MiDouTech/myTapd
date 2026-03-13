package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工单流转历史输出
 */
@Data
public class TicketFlowRecordOutput implements Serializable {

    private Long id;

    private Long ticketId;

    private String ticketNo;

    /** 流转类型（TRANSIT/TRANSFER/RETURN/ASSIGN/CLOSE） */
    private String flowType;

    /** 流转类型显示名 */
    private String flowTypeLabel;

    private String transitionId;

    private String transitionName;

    private String fromStatus;

    private String fromStatusName;

    private String toStatus;

    private String toStatusName;

    private Long fromAssigneeId;

    private String fromAssigneeName;

    private Long toAssigneeId;

    private String toAssigneeName;

    private Long operatorId;

    private String operatorName;

    private String operatorRole;

    private String remark;

    private Date createTime;
}
