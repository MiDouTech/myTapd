package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工单审批任务及历史输出
 */
@Data
public class ApprovalTaskOutput implements Serializable {

    /** 工单ID */
    private Long ticketId;

    /** 审批任务列表（按节点分组） */
    private List<ApprovalNodeGroup> nodes;

    /** 审批操作历史时间轴 */
    private List<ApprovalRecordItem> records;

    /** 当前是否有 pending 任务（用于前端显示操作按钮） */
    private Boolean hasPendingTask;

    /** 当前登录用户的待审批任务ID（null=无需操作） */
    private Long myPendingTaskId;

    @Data
    public static class ApprovalNodeGroup implements Serializable {
        private String nodeKey;
        private String nodeName;
        private String approveMode;
        private List<TaskItem> tasks;
    }

    @Data
    public static class TaskItem implements Serializable {
        private Long taskId;
        private Long assigneeId;
        private String assigneeName;
        private String taskStatus;
        private String taskStatusLabel;
        private String remark;
        private Date operateTime;
        private Date dueTime;
        private Integer sortOrder;
    }

    @Data
    public static class ApprovalRecordItem implements Serializable {
        private Long recordId;
        private Long taskId;
        private String nodeKey;
        private String nodeName;
        private String actionType;
        private String actionLabel;
        private Long operatorId;
        private String operatorName;
        private String remark;
        private Long targetAssigneeId;
        private String targetAssigneeName;
        private Date createTime;
    }
}
