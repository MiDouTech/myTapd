package com.miduo.cloud.ticket.domain.workflow.service;

import com.alibaba.fastjson2.JSON;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowState;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 状态机工作流引擎实现
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

        return transitions.stream()
                .filter(t -> t.getFrom().equals(currentStatus))
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

        return transitions.stream()
                .anyMatch(t -> t.getFrom().equals(currentStatus)
                        && t.getTo().equals(targetStatus)
                        && isRoleAllowed(t, userRole));
    }

    @Override
    public List<WorkflowState> parseStates(String statesJson) {
        if (statesJson == null || statesJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(statesJson, WorkflowState.class);
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
            return JSON.parseArray(transitionsJson, WorkflowTransition.class);
        } catch (Exception e) {
            log.error("解析工作流流转规则失败: {}", transitionsJson, e);
            return Collections.emptyList();
        }
    }

    @Override
    public WorkflowState findState(String statesJson, String statusCode) {
        List<WorkflowState> states = parseStates(statesJson);
        return states.stream()
                .filter(s -> s.getCode().equals(statusCode))
                .findFirst()
                .orElse(null);
    }

    private boolean isRoleAllowed(WorkflowTransition transition, String userRole) {
        if (transition.getAllowedRoles() == null || transition.getAllowedRoles().isEmpty()) {
            return true;
        }
        return transition.getAllowedRoles().contains(userRole);
    }
}
