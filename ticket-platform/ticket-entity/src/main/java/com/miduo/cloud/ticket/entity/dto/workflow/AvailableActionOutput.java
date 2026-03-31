package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 可用操作输出
 * 供前端动态渲染操作按钮，不再硬编码状态选项
 */
@Data
public class AvailableActionOutput implements Serializable {

    /** 工单ID */
    private Long ticketId;

    /** 当前状态码 */
    private String currentStatus;

    /** 当前状态显示名 */
    private String currentStatusName;

    /** 当前状态是否为终态 */
    private Boolean isTerminal;

    /** 可执行的操作列表 */
    private List<ActionItem> actions;

    /** 当前工单可用的所有状态列表（含已走过的，供看板/进度展示） */
    private List<StatusItem> allStatuses;

    @Data
    public static class ActionItem implements Serializable {

        /** 流转规则ID（可用于精确触发，如 t01） */
        private String transitionId;

        /** 目标状态码 */
        private String targetStatus;

        /** 目标状态显示名 */
        private String targetStatusName;

        /** 操作动作名称（如：受理、处理完成） */
        private String actionName;

        /** 是否为退回操作 */
        private Boolean isReturn;

        /** 是否必须填写备注 */
        private Boolean requireRemark;

        /** 是否支持在流转时同步变更处理人 */
        private Boolean allowTransfer;

        /** 允许执行此操作的角色（前端可据此做权限提示） */
        private List<String> allowedRoles;
    }

    @Data
    public static class StatusItem implements Serializable {

        /** 状态码 */
        private String code;

        /** 状态显示名 */
        private String name;

        /** 状态类型（INITIAL/INTERMEDIATE/TERMINAL） */
        private String type;

        /** 是否为当前状态 */
        private Boolean isCurrent;

        /** 排列顺序 */
        private Integer order;
    }
}
