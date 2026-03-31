package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 状态流转请求
 * 支持两种触发方式：
 * 1. 按 transitionId 精确触发（推荐，前端从 getAvailableActions 接口获取）
 * 2. 按 targetStatus 触发（兼容模式）
 */
@Data
public class TransitInput implements Serializable {

    /**
     * 流转规则ID（如 t01、t02），精确触发时使用
     * 与 targetStatus 二选一，transitionId 优先
     */
    private String transitionId;

    /**
     * 目标状态码（如 processing、pending_verify）
     * transitionId 为空时使用
     */
    private String targetStatus;

    /**
     * 备注/原因
     * requireRemark=true 的流转必须提供
     */
    private String remark;

    /**
     * 流转时同步指定新的处理人（allowTransfer=true 的流转可选填）
     */
    private Long newAssigneeId;

    /**
     * 流转时同步指定多名处理人（首位为主处理人）；非空时优先于 newAssigneeId
     */
    private List<Long> newAssigneeIds;

    /**
     * 缺陷工单：确认缺陷转开发时可选填，与测试信息中的复现环境二选一必填（PRODUCTION/TEST/BOTH）
     */
    private String reproduceEnv;

    /**
     * 缺陷工单：进入临时解决时必填（可填 ISO 日期时间字符串，如 2026-04-01T18:00:00），与开发信息中已保存的计划时间二选一
     */
    private String plannedFullResolveAt;
}
