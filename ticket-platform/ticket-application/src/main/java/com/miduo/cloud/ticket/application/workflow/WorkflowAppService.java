package com.miduo.cloud.ticket.application.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.WorkflowMode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowState;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition;
import com.miduo.cloud.ticket.domain.workflow.service.WorkflowEngine;
import com.miduo.cloud.ticket.entity.dto.workflow.WorkflowDetailOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.WorkflowListOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.WorkflowMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流应用服务
 */
@Service
public class WorkflowAppService extends BaseApplicationService {

    private final WorkflowMapper workflowMapper;
    private final WorkflowEngine workflowEngine;

    public WorkflowAppService(WorkflowMapper workflowMapper, WorkflowEngine workflowEngine) {
        this.workflowMapper = workflowMapper;
        this.workflowEngine = workflowEngine;
    }

    /**
     * 查询工作流列表
     */
    public List<WorkflowListOutput> listWorkflows() {
        LambdaQueryWrapper<WorkflowPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowPO::getIsActive, 1)
                .orderByAsc(WorkflowPO::getId);
        List<WorkflowPO> workflows = workflowMapper.selectList(wrapper);

        List<WorkflowListOutput> result = new ArrayList<>();
        for (WorkflowPO po : workflows) {
            WorkflowListOutput output = new WorkflowListOutput();
            output.setId(po.getId());
            output.setName(po.getName());
            output.setMode(po.getMode());
            WorkflowMode mode = WorkflowMode.fromCode(po.getMode());
            output.setModeLabel(mode != null ? mode.getLabel() : po.getMode());
            output.setDescription(po.getDescription());
            output.setIsBuiltin(po.getIsBuiltin());
            output.setIsActive(po.getIsActive());
            output.setCreateTime(po.getCreateTime());

            List<WorkflowState> states = workflowEngine.parseStates(po.getStates());
            List<WorkflowTransition> transitions = workflowEngine.parseTransitions(po.getTransitions());
            output.setStateCount(states.size());
            output.setTransitionCount(transitions.size());

            result.add(output);
        }
        return result;
    }

    /**
     * 查询工作流详情
     */
    public WorkflowDetailOutput getWorkflowDetail(Long id) {
        WorkflowPO po = workflowMapper.selectById(id);
        if (po == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_NOT_FOUND);
        }

        WorkflowDetailOutput output = new WorkflowDetailOutput();
        output.setId(po.getId());
        output.setName(po.getName());
        output.setMode(po.getMode());
        WorkflowMode mode = WorkflowMode.fromCode(po.getMode());
        output.setModeLabel(mode != null ? mode.getLabel() : po.getMode());
        output.setDescription(po.getDescription());
        output.setIsBuiltin(po.getIsBuiltin());
        output.setIsActive(po.getIsActive());
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());

        List<WorkflowState> states = workflowEngine.parseStates(po.getStates());
        Map<String, String> stateNameMap = states.stream()
                .collect(Collectors.toMap(WorkflowState::getCode, WorkflowState::getName));

        List<WorkflowDetailOutput.StateItem> stateItems = new ArrayList<>();
        for (WorkflowState state : states) {
            WorkflowDetailOutput.StateItem item = new WorkflowDetailOutput.StateItem();
            item.setCode(state.getCode());
            item.setName(state.getName());
            item.setType(state.getType());
            item.setSlaAction(state.getSlaAction());
            stateItems.add(item);
        }
        output.setStates(stateItems);

        List<WorkflowTransition> transitions = workflowEngine.parseTransitions(po.getTransitions());
        List<WorkflowDetailOutput.TransitionItem> transitionItems = new ArrayList<>();
        for (WorkflowTransition transition : transitions) {
            WorkflowDetailOutput.TransitionItem item = new WorkflowDetailOutput.TransitionItem();
            item.setFrom(transition.getFrom());
            item.setFromName(stateNameMap.getOrDefault(transition.getFrom(), transition.getFrom()));
            item.setTo(transition.getTo());
            item.setToName(stateNameMap.getOrDefault(transition.getTo(), transition.getTo()));
            item.setName(transition.getName());
            item.setAllowedRoles(transition.getAllowedRoles());
            transitionItems.add(item);
        }
        output.setTransitions(transitionItems);

        return output;
    }

    /**
     * 根据ID获取工作流PO
     */
    public WorkflowPO getWorkflowById(Long id) {
        WorkflowPO po = workflowMapper.selectById(id);
        if (po == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        return po;
    }
}
