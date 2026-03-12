package com.miduo.cloud.ticket.application.workflow;

import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowState;
import com.miduo.cloud.ticket.domain.workflow.service.WorkflowEngine;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.WorkflowMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 工作流应用服务
 * 提供工作流状态查询、流转校验等基础能力（委托给 WorkflowEngine）
 */
@Service
public class WorkflowApplicationService {

    @Resource
    private WorkflowMapper workflowMapper;

    @Resource
    private WorkflowEngine workflowEngine;

    /**
     * 校验状态流转合法性（不含角色校验，兼容旧调用方）
     */
    public void validateTransition(Long workflowId, String fromStatus, String toStatus) {
        WorkflowPO workflow = requireWorkflow(workflowId);
        boolean valid = workflowEngine.validate(
                workflow.getTransitions(),
                fromStatus.toLowerCase(),
                toStatus.toLowerCase(),
                null
        );
        if (!valid) {
            throw BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                    "不允许从状态[" + fromStatus + "]流转到[" + toStatus + "]");
        }
    }

    /**
     * 获取工作流初始状态码
     * 基于工作流 JSON 中 type=INITIAL 的状态
     */
    public String getInitialStatus(Long workflowId) {
        if (workflowId == null) {
            return TicketStatus.PENDING_ACCEPT.getCode();
        }
        WorkflowPO workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            return TicketStatus.PENDING_ACCEPT.getCode();
        }
        String initial = workflowEngine.getInitialStatus(workflow.getStates());
        return initial != null ? initial : TicketStatus.PENDING_ACCEPT.getCode();
    }

    /**
     * 判断指定状态是否为终态
     */
    public boolean isTerminalStatus(Long workflowId, String status) {
        if (workflowId == null || status == null) {
            return false;
        }
        WorkflowPO workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            return false;
        }
        WorkflowState state = workflowEngine.findState(workflow.getStates(), status.toLowerCase());
        return state != null && state.isTerminal();
    }

    /**
     * 获取工作流PO，不存在则抛出异常
     */
    private WorkflowPO requireWorkflow(Long workflowId) {
        if (workflowId == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        WorkflowPO workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        return workflow;
    }
}
