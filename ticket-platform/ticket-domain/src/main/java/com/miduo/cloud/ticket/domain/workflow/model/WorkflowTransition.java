package com.miduo.cloud.ticket.domain.workflow.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工作流流转规则值对象
 * 对应工作流 JSON 中的 transitions 数组元素
 */
@Data
public class WorkflowTransition implements Serializable {

    /** 流转规则ID（工作流内唯一，如 t01、t02） */
    private String id;

    /** 来源状态码 */
    private String from;

    /** 目标状态码 */
    private String to;

    /** 流转动作名称（如：受理、处理完成、验收通过） */
    private String name;

    /**
     * 允许执行此流转的角色列表
     * 取值：SUBMITTER / HANDLER / ADMIN / TICKET_ADMIN
     * 空列表表示所有角色均可操作
     */
    private List<String> allowedRoles;

    /**
     * 是否需要填写备注/原因（前端控制必填项）
     * true - 必须填写备注才能执行此流转
     */
    private Boolean requireRemark;

    /**
     * 是否允许在流转时同步变更处理人（转派到下一节点）
     * true - 前端展示处理人选择框
     */
    private Boolean allowTransfer;

    /**
     * 是否为退回流转
     * true - 状态流转方向是向上游退回（用于前端特殊展示）
     */
    private Boolean isReturn;

    /**
     * 是否需要走审批任务层（嵌入式审批引擎，借鉴米多星球设计）
     * true  - 本次 transition 执行后不立即推进到 to 状态，
     *         而是创建审批任务；审批全部通过后才自动流转到 approvalConfig.passedStatus
     * false/null - 普通 FSM 流转，行为不变
     */
    private Boolean requireApproval;

    /**
     * 审批流配置（requireApproval=true 时必填）
     * 包含审批节点列表和通过/驳回后的目标状态
     */
    private ApprovalConfig approvalConfig;

    public boolean isReturnTransition() {
        return Boolean.TRUE.equals(isReturn);
    }

    public boolean needsApproval() {
        return Boolean.TRUE.equals(requireApproval) && approvalConfig != null;
    }

    // =========================================================
    // 嵌套类：审批流配置（对应米多星球 node_config 中的 flowSteps）
    // =========================================================

    /**
     * 审批流总体配置
     */
    @Data
    public static class ApprovalConfig implements Serializable {

        /**
         * 审批全部通过后，自动流转到的目标状态码
         * （不再需要用户手动点击"审批通过"流转按钮）
         */
        private String passedStatus;

        /**
         * 审批被驳回后，自动流转到的目标状态码
         */
        private String rejectedStatus;

        /**
         * 审批节点列表（按顺序排列）
         * sequential 模式下：前一节点全部通过后，下一节点才激活
         * countersign/orsign/single 模式下：同一节点的多个审批人并行处理
         */
        private List<ApprovalNode> nodes;
    }

    /**
     * 单个审批节点配置（对应米多星球 FlowNode）
     */
    @Data
    public static class ApprovalNode implements Serializable {

        /** 节点标识（同一 transition 内唯一，如 node_01） */
        private String nodeKey;

        /** 节点名称（如：部门负责人审批） */
        private String nodeName;

        /**
         * 审批模式：single/countersign/orsign/sequential
         * 对应 ApprovalMode 枚举
         */
        private String approveMode;

        /**
         * 审批人来源类型
         * member     - 指定用户（使用 assigneeIds 列表）
         * groupLeader - 工单所属处理组的组长
         */
        private String assigneeType;

        /**
         * 当 assigneeType=member 时，指定的审批人用户ID列表
         */
        private List<Long> assigneeIds;

        /**
         * 审批超时时间（小时），超时后发出催办通知（可选）
         */
        private Integer dueHours;
    }
}
