package com.miduo.cloud.ticket.domain.workflow.service;

import com.alibaba.fastjson2.JSON;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowState;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 有限状态机（FSM）工作流引擎实现
 * 核心设计原则：
 * 1. 所有状态流转必须经过此引擎校验，不存在绕过路径
 * 2. 状态码大小写不敏感（统一 toLowerCase 处理）
 * 3. 角色精确匹配（SUBMITTER/HANDLER/ADMIN/TICKET_ADMIN）
 */
public class StateMachineWorkflowEngine implements WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(StateMachineWorkflowEngine.class);

    @Override
    public boolean transit(String statesJson, String transitionsJson,
                           String currentStatus, String targetStatus, String userRole) {
        if (!validate(transitionsJson, currentStatus, targetStatus, userRole)) {
            return false;
        }
        WorkflowState targetState = findState(statesJson, targetStatus);
        return targetState != null;
    }

    @Override
    public List<WorkflowTransition> getAvailableActions(String statesJson, String transitionsJson,
                                                         String currentStatus, String userRole) {
        List<WorkflowTransition> transitions = parseTransitions(transitionsJson);
        if (transitions == null || transitions.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedCurrent = normalize(currentStatus);
        return transitions.stream()
                .filter(t -> normalize(t.getFrom()).equals(normalizedCurrent))
                .filter(t -> isRoleAllowed(t, userRole))
                .collect(Collectors.toList());
    }

    @Override
    public boolean validate(String transitionsJson, String currentStatus,
                            String targetStatus, String userRole) {
        List<WorkflowTransition> transitions = parseTransitions(transitionsJson);
        if (transitions == null || transitions.isEmpty()) {
            log.warn("工作流流转规则为空");
            return false;
        }

        String normalizedCurrent = normalize(currentStatus);
        String normalizedTarget = normalize(targetStatus);

        return transitions.stream()
                .anyMatch(t -> normalize(t.getFrom()).equals(normalizedCurrent)
                        && normalize(t.getTo()).equals(normalizedTarget)
                        && isRoleAllowed(t, userRole));
    }

    @Override
    public WorkflowTransition validateByTransitionId(String transitionsJson, String transitionId,
                                                      String currentStatus, String userRole) {
        if (transitionId == null || transitionId.trim().isEmpty()) {
            return null;
        }
        List<WorkflowTransition> transitions = parseTransitions(transitionsJson);
        if (transitions == null || transitions.isEmpty()) {
            return null;
        }

        String normalizedCurrent = normalize(currentStatus);

        return transitions.stream()
                .filter(t -> transitionId.equals(t.getId()))
                .filter(t -> normalize(t.getFrom()).equals(normalizedCurrent))
                .filter(t -> isRoleAllowed(t, userRole))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<WorkflowState> parseStates(String statesJson) {
        if (statesJson == null || statesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<WorkflowState> states = JSON.parseArray(statesJson, WorkflowState.class);
            if (states == null) {
                return Collections.emptyList();
            }
            return states.stream()
                    .sorted(Comparator.comparingInt(s -> s.getOrder() != null ? s.getOrder() : 999))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("解析工作流状态定义失败: {}", statesJson, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<WorkflowTransition> parseTransitions(String transitionsJson) {
        if (transitionsJson == null || transitionsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<WorkflowTransition> transitions = JSON.parseArray(transitionsJson, WorkflowTransition.class);
            return transitions != null ? transitions : Collections.emptyList();
        } catch (Exception e) {
            log.error("解析工作流流转规则失败: {}", transitionsJson, e);
            return Collections.emptyList();
        }
    }

    @Override
    public WorkflowState findState(String statesJson, String statusCode) {
        String normalized = normalize(statusCode);
        return parseStates(statesJson).stream()
                .filter(s -> normalize(s.getCode()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getInitialStatus(String statesJson) {
        return parseStates(statesJson).stream()
                .filter(WorkflowState::isInitial)
                .map(WorkflowState::getCode)
                .findFirst()
                .orElse(null);
    }

    /**
     * 状态码归一化（小写，去除首尾空格）
     */
    private String normalize(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toLowerCase();
    }

    private boolean isRoleAllowed(WorkflowTransition transition, String userRole) {
        if (transition.getAllowedRoles() == null || transition.getAllowedRoles().isEmpty()) {
            return true;
        }
        return transition.getAllowedRoles().contains(userRole);
    }
}
