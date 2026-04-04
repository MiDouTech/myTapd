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

    public boolean isReturnTransition() {
        return Boolean.TRUE.equals(isReturn);
    }
}
