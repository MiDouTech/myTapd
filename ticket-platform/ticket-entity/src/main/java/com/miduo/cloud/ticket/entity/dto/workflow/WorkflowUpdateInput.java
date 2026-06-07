package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 工作流更新请求（自定义工作流；内置工作流禁止修改）
 */
@Data
public class WorkflowUpdateInput implements Serializable {

    @NotBlank(message = "工作流名称不能为空")
    private String name;

    @NotBlank(message = "工作流模式不能为空")
    private String mode;

    private String description;

    @NotNull(message = "启用状态不能为空")
    private Integer isActive;

    @NotEmpty(message = "状态定义不能为空")
    @Valid
    private List<StateItemInput> states;

    @NotEmpty(message = "流转规则不能为空")
    @Valid
    private List<TransitionItemInput> transitions;

    @Data
    public static class StateItemInput implements Serializable {

        @NotBlank(message = "状态编码不能为空")
        private String code;

        @NotBlank(message = "状态名称不能为空")
        private String name;

        @NotBlank(message = "状态类型不能为空")
        private String type;

        private String slaAction;

        private Integer order;
    }

    @Data
    public static class TransitionItemInput implements Serializable {

        private String id;

        @NotBlank(message = "流转起始状态不能为空")
        private String from;

        @NotBlank(message = "流转目标状态不能为空")
        private String to;

        @NotBlank(message = "流转名称不能为空")
        private String name;

        private List<String> allowedRoles;

        private Boolean requireRemark;

        private Boolean allowTransfer;

        private Boolean isReturn;

        /**
         * 是否需要走审批任务层（借鉴米多星球审批引擎）
         * true 时需同时配置 approvalConfig
         */
        private Boolean requireApproval;

        /**
         * 审批节点配置（requireApproval=true 时填写）
         * 保存为 workflow.transitions JSON 的 approvalConfig 字段
         */
        private ApprovalConfigInput approvalConfig;
    }

    /**
     * 审批流配置输入（对应 WorkflowTransition.ApprovalConfig）
     */
    @Data
    public static class ApprovalConfigInput implements Serializable {

        /** 审批全部通过后自动流转到的目标状态码 */
        private String passedStatus;

        /** 审批被驳回后自动流转到的目标状态码 */
        private String rejectedStatus;

        /** 审批节点列表 */
        private List<ApprovalNodeInput> nodes;
    }

    /**
     * 单个审批节点配置输入（对应 WorkflowTransition.ApprovalNode）
     */
    @Data
    public static class ApprovalNodeInput implements Serializable {

        /** 节点标识（同一 transition 内唯一） */
        private String nodeKey;

        /** 节点名称 */
        private String nodeName;

        /**
         * 审批模式：single / countersign / orsign / sequential
         */
        private String approveMode;

        /**
         * 审批人来源：member（指定人）/ groupLeader（处理组长）
         */
        private String assigneeType;

        /**
         * assigneeType=member 时，指定的审批人用户ID列表
         */
        private List<Long> assigneeIds;

        /** 审批超时时间（小时），可选 */
        private Integer dueHours;
    }
}
