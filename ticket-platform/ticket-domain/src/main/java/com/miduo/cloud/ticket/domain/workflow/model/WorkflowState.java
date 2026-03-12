package com.miduo.cloud.ticket.domain.workflow.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 工作流状态值对象
 * 对应工作流 JSON 中的 states 数组元素
 */
@Data
public class WorkflowState implements Serializable {

    /** 状态码（小写下划线，与 TicketStatus.code 保持一致） */
    private String code;

    /** 状态显示名称 */
    private String name;

    /**
     * 状态类型
     * INITIAL    - 初始状态（工单创建后进入的第一个状态）
     * INTERMEDIATE - 中间状态
     * TERMINAL   - 终态（完成/关闭/驳回等）
     */
    private String type;

    /**
     * SLA 动作指令
     * START_RESPONSE - 开始计算首次响应时间
     * START_RESOLVE  - 开始计算解决时间
     * PAUSE          - 暂停 SLA 计时（如待验收、已挂起）
     * STOP           - 停止 SLA 计时（终态）
     */
    private String slaAction;

    /** 状态排列顺序（越小越靠前，用于前端展示排序） */
    private Integer order;

    /**
     * 是否终态
     */
    public boolean isTerminal() {
        return "TERMINAL".equals(this.type);
    }

    /**
     * 是否初始状态
     */
    public boolean isInitial() {
        return "INITIAL".equals(this.type);
    }
}
