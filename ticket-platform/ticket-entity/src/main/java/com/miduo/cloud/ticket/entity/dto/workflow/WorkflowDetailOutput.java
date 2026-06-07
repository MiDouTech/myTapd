package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工作流详情输出
 */
@Data
public class WorkflowDetailOutput implements Serializable {

    private Long id;
    private String name;
    private String mode;
    private String modeLabel;
    private String description;
    private Integer isBuiltin;
    private Integer isActive;
    private Long invocationCount;
    private Boolean canDelete;
    private String deleteBlockedReason;
    private List<StateItem> states;
    private List<TransitionItem> transitions;
    private Date createTime;
    private Date updateTime;

    @Data
    public static class StateItem implements Serializable {
        private String code;
        private String name;
        private String type;
        private String slaAction;
        private Integer order;
    }

    @Data
    public static class TransitionItem implements Serializable {
        private String id;
        private String from;
        private String fromName;
        private String to;
        private String toName;
        private String name;
        private List<String> allowedRoles;
        private Boolean requireRemark;
        private Boolean allowTransfer;
        private Boolean isReturn;
        /** 是否需要走审批任务层（借鉴米多星球审批引擎） */
        private Boolean requireApproval;
        /** 审批节点配置（requireApproval=true 时有值） */
        private ApprovalConfigOutput approvalConfig;
    }

    /** 审批流配置输出（独立 DTO，避免跨模块依赖） */
    @Data
    public static class ApprovalConfigOutput implements Serializable {
        private String passedStatus;
        private String rejectedStatus;
        private List<ApprovalNodeOutput> nodes;
    }

    /** 审批节点输出 */
    @Data
    public static class ApprovalNodeOutput implements Serializable {
        private String nodeKey;
        private String nodeName;
        private String approveMode;
        private String assigneeType;
        private List<Long> assigneeIds;
        private Integer dueHours;
    }
}
