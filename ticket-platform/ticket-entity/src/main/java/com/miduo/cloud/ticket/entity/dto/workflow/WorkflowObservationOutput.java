package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工作流运行观察输出
 */
@Data
public class WorkflowObservationOutput implements Serializable {

    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 工作流名称
     */
    private String workflowName;

    /**
     * 当前使用该工作流的工单数
     */
    private Long ticketCount;

    /**
     * 最近流转记录
     */
    private List<RecentFlowItem> recentFlows;

    /**
     * 节点耗时统计
     */
    private List<NodeObservationItem> nodeStats;

    @Data
    public static class RecentFlowItem implements Serializable {
        private Long id;
        private Long ticketId;
        private String ticketNo;
        private String flowType;
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
        private java.util.Date createTime;
    }

    @Data
    public static class NodeObservationItem implements Serializable {
        private String nodeCode;
        private String nodeName;
        private String nodeType;
        private Integer order;
        private Long ticketCount;
        private Long avgWaitDurationSec;
        private Long avgProcessDurationSec;
        private Long avgTotalDurationSec;
        private Long maxTotalDurationSec;
    }
}
