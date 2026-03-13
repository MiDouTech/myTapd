package com.miduo.cloud.ticket.domain.workflow.service;

import com.miduo.cloud.ticket.domain.workflow.model.WorkflowState;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition;

import java.util.List;

/**
 * 工作流引擎接口
 * 基于有限状态机（FSM）模型，驱动工单状态流转
 */
public interface WorkflowEngine {

    /**
     * 执行状态流转（校验并返回结果）
     *
     * @param statesJson      状态定义JSON
     * @param transitionsJson 流转规则JSON
     * @param currentStatus   当前状态码
     * @param targetStatus    目标状态码
     * @param userRole        操作人角色
     * @return 流转是否成功
     */
    boolean transit(String statesJson, String transitionsJson,
                    String currentStatus, String targetStatus, String userRole);

    /**
     * 获取当前状态可用的操作列表（基于操作人角色过滤）
     *
     * @param statesJson      状态定义JSON
     * @param transitionsJson 流转规则JSON
     * @param currentStatus   当前状态码
     * @param userRole        操作人角色
     * @return 可用的流转列表
     */
    List<WorkflowTransition> getAvailableActions(String statesJson, String transitionsJson,
                                                  String currentStatus, String userRole);

    /**
     * 校验流转是否合法（不执行流转，仅验证权限和规则）
     *
     * @param transitionsJson 流转规则JSON
     * @param currentStatus   当前状态码
     * @param targetStatus    目标状态码
     * @param userRole        操作人角色
     * @return 校验结果
     */
    boolean validate(String transitionsJson, String currentStatus,
                     String targetStatus, String userRole);

    /**
     * 根据流转ID校验（精确匹配）
     *
     * @param transitionsJson 流转规则JSON
     * @param transitionId    流转规则ID
     * @param currentStatus   当前状态码
     * @param userRole        操作人角色
     * @return 找到的流转规则，不存在或无权限返回 null
     */
    WorkflowTransition validateByTransitionId(String transitionsJson, String transitionId,
                                               String currentStatus, String userRole);

    /**
     * 解析状态定义JSON
     *
     * @param statesJson 状态定义JSON
     * @return 状态列表（按 order 排序）
     */
    List<WorkflowState> parseStates(String statesJson);

    /**
     * 解析流转规则JSON
     *
     * @param transitionsJson 流转规则JSON
     * @return 流转规则列表
     */
    List<WorkflowTransition> parseTransitions(String transitionsJson);

    /**
     * 根据状态码查找状态定义
     *
     * @param statesJson  状态定义JSON
     * @param statusCode  状态码
     * @return 状态定义，不存在返回 null
     */
    WorkflowState findState(String statesJson, String statusCode);

    /**
     * 获取工作流初始状态码
     *
     * @param statesJson 状态定义JSON
     * @return 初始状态码，不存在返回 null
     */
    String getInitialStatus(String statesJson);
}
