package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单状态枚举
 * 状态码与数据库存储值一致（均为小写下划线格式）
 * 通用工单 + 缺陷工单的所有状态定义
 */
@Getter
@AllArgsConstructor
public enum TicketStatus {

    // ---- 通用工单状态 ----
    PENDING_ASSIGN("pending_assign", "待分派"),
    PENDING_ACCEPT("pending_accept", "待受理"),
    PROCESSING("processing", "处理中"),
    SUSPENDED("suspended", "已挂起"),
    PENDING_VERIFY("pending_verify", "待验收"),
    COMPLETED("completed", "已完成"),
    CLOSED("closed", "已关闭"),

    // ---- 缺陷工单专属状态 ----
    PENDING_TEST_ACCEPT("pending_test_accept", "待测试受理"),
    /** 缺陷流程中展示为「测试复现中」，状态码保持 testing 以兼容历史数据 */
    TESTING("testing", "测试复现中"),
    /** 已并入 testing（测试复现中），仅兼容历史库中残留状态 */
    INVESTIGATING("investigating", "排查中"),
    PENDING_DEV_ACCEPT("pending_dev_accept", "待开发受理"),
    DEVELOPING("developing", "开发解决中"),
    TEMP_RESOLVED("temp_resolved", "临时解决"),
    /** 已废弃节点，仅兼容历史数据；新流程验收后直达已完成 */
    PENDING_CS_CONFIRM("pending_cs_confirm", "待客服确认"),

    // ---- 审批工单专属状态 ----
    SUBMITTED("submitted", "已提交"),
    DEPT_APPROVAL("dept_approval", "部门审批"),
    EXECUTING("executing", "执行中"),
    REJECTED("rejected", "已驳回");

    private final String code;
    private final String label;

    /**
     * 终态判断
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CLOSED || this == REJECTED;
    }

    /**
     * 是否为「待受理/待分派」类中间态（含缺陷流程的待测试受理、待开发受理）。
     * 用于通知文案等场景：此类状态下通常需要展示当前处理人。
     */
    public boolean isPendingAcceptanceLike() {
        return this == PENDING_ASSIGN
                || this == PENDING_ACCEPT
                || this == PENDING_TEST_ACCEPT
                || this == PENDING_DEV_ACCEPT;
    }

    /**
     * 根据 code 查找枚举，大小写不敏感，支持历史别名映射
     */
    public static TicketStatus fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String normalized = code.trim().toLowerCase();
        // 历史遗留别名兼容（数据库中可能存在的旧值）
        switch (normalized) {
            case "pending":          return PENDING_ACCEPT;
            case "pending_dispatch": return PENDING_ASSIGN;
            case "pending_test":     return PENDING_TEST_ACCEPT;
            case "pending_dev":      return PENDING_DEV_ACCEPT;
            default:
                break;
        }
        for (TicketStatus status : values()) {
            if (status.code.equals(normalized)) {
                return status;
            }
        }
        return null;
    }
}
