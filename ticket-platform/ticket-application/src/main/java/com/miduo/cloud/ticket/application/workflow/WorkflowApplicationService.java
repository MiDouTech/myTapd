package com.miduo.cloud.ticket.application.workflow;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.WorkflowMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class WorkflowApplicationService {

    @Resource
    private WorkflowMapper workflowMapper;

    public void validateTransition(Long workflowId, String fromStatus, String toStatus) {
        if (workflowId == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        WorkflowPO workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_NOT_FOUND);
        }

        JSONArray transitions = JSON.parseArray(workflow.getTransitions());
        if (transitions == null || transitions.isEmpty()) {
            throw BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID, "工作流未配置流转规则");
        }

        boolean validTransition = false;
        for (int i = 0; i < transitions.size(); i++) {
            JSONObject transition = transitions.getJSONObject(i);
            String from = transition.getString("from");
            String to = transition.getString("to");
            if (fromStatus.equals(from) && toStatus.equals(to)) {
                validTransition = true;
                break;
            }
        }

        if (!validTransition) {
            throw BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                    "不允许从状态[" + fromStatus + "]流转到[" + toStatus + "]");
        }
    }

    public String getInitialStatus(Long workflowId) {
        if (workflowId == null) {
            return "PENDING";
        }
        WorkflowPO workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            return "PENDING";
        }

        JSONArray states = JSON.parseArray(workflow.getStates());
        if (states == null || states.isEmpty()) {
            return "PENDING";
        }

        for (int i = 0; i < states.size(); i++) {
            JSONObject state = states.getJSONObject(i);
            if ("INITIAL".equals(state.getString("type"))) {
                return state.getString("code");
            }
        }
        return "PENDING";
    }

    public boolean isTerminalStatus(Long workflowId, String status) {
        if (workflowId == null || status == null) {
            return false;
        }
        WorkflowPO workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            return false;
        }

        JSONArray states = JSON.parseArray(workflow.getStates());
        if (states == null || states.isEmpty()) {
            return false;
        }

        for (int i = 0; i < states.size(); i++) {
            JSONObject state = states.getJSONObject(i);
            if (status.equals(state.getString("code")) && "TERMINAL".equals(state.getString("type"))) {
                return true;
            }
        }
        return false;
    }
}
