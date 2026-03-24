package com.miduo.cloud.ticket.application.workflow;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.UserRole;
import com.miduo.cloud.ticket.common.enums.WorkflowMode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowState;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition;
import com.miduo.cloud.ticket.domain.workflow.service.WorkflowEngine;
import com.miduo.cloud.ticket.entity.dto.workflow.WorkflowDetailOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.WorkflowListOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.WorkflowUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.WorkflowMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
        wrapper.orderByAsc(WorkflowPO::getId);
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
            output.setUpdateTime(po.getUpdateTime());

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
     * 更新工作流定义（仅非内置工作流）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkflow(Long id, WorkflowUpdateInput input) {
        WorkflowPO existing = workflowMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_NOT_FOUND);
        }
        if (existing.getIsBuiltin() != null && existing.getIsBuiltin() == 1) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "内置工作流不允许修改");
        }
        if (WorkflowMode.fromCode(input.getMode()) == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "工作流模式不合法");
        }
        if (input.getIsActive() != 0 && input.getIsActive() != 1) {
            throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "启用状态只能为0或1");
        }

        List<WorkflowState> states = buildStatesForUpdate(input.getStates());
        List<WorkflowTransition> transitions = buildTransitionsForUpdate(input.getTransitions(), states);
        validateWorkflowGraph(states, transitions);

        LambdaQueryWrapper<WorkflowPO> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(WorkflowPO::getName, input.getName().trim())
                .ne(WorkflowPO::getId, id);
        if (workflowMapper.selectCount(nameCheck) > 0) {
            throw BusinessException.of(ErrorCode.DATA_ALREADY_EXISTS, "工作流名称已存在");
        }

        existing.setName(input.getName().trim());
        existing.setMode(input.getMode().trim());
        existing.setDescription(input.getDescription() != null ? input.getDescription().trim() : null);
        existing.setIsActive(input.getIsActive());
        existing.setStates(JSON.toJSONString(states));
        existing.setTransitions(JSON.toJSONString(transitions));
        workflowMapper.updateById(existing);
    }

    private List<WorkflowState> buildStatesForUpdate(List<WorkflowUpdateInput.StateItemInput> items) {
        List<WorkflowState> states = new ArrayList<>();
        for (WorkflowUpdateInput.StateItemInput item : items) {
            if (item == null) {
                continue;
            }
            WorkflowState s = new WorkflowState();
            s.setCode(item.getCode() != null ? item.getCode().trim() : null);
            s.setName(item.getName() != null ? item.getName().trim() : null);
            s.setType(item.getType() != null ? item.getType().trim().toUpperCase(Locale.ROOT) : null);
            s.setSlaAction(item.getSlaAction() != null ? item.getSlaAction().trim() : null);
            s.setOrder(item.getOrder());
            states.add(s);
        }
        return states;
    }

    private List<WorkflowTransition> buildTransitionsForUpdate(
            List<WorkflowUpdateInput.TransitionItemInput> items,
            List<WorkflowState> states) {
        Set<String> stateCodes = states.stream()
                .map(s -> normalizeStateCode(s.getCode()))
                .collect(Collectors.toSet());

        Set<String> usedIds = new HashSet<>();
        int autoSeq = 1;
        List<WorkflowTransition> transitions = new ArrayList<>();
        for (WorkflowUpdateInput.TransitionItemInput item : items) {
            if (item == null) {
                continue;
            }
            WorkflowTransition t = new WorkflowTransition();
            String from = item.getFrom() != null ? item.getFrom().trim() : "";
            String to = item.getTo() != null ? item.getTo().trim() : "";
            if (!stateCodes.contains(normalizeStateCode(from))
                    || !stateCodes.contains(normalizeStateCode(to))) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED,
                        "流转规则的来源或目标状态编码不存在于状态定义中");
            }
            t.setFrom(from);
            t.setTo(to);
            t.setName(item.getName() != null ? item.getName().trim() : "");
            t.setAllowedRoles(normalizeAllowedRoles(item.getAllowedRoles()));
            t.setRequireRemark(item.getRequireRemark());
            t.setAllowTransfer(item.getAllowTransfer());
            t.setIsReturn(item.getIsReturn());

            String tid = item.getId() != null ? item.getId().trim() : "";
            if (tid.isEmpty() || usedIds.contains(tid)) {
                tid = "t_auto_" + autoSeq++;
                while (usedIds.contains(tid)) {
                    tid = "t_auto_" + autoSeq++;
                }
            }
            usedIds.add(tid);
            t.setId(tid);
            transitions.add(t);
        }
        return transitions;
    }

    private List<String> normalizeAllowedRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String r : roles) {
            if (r == null) {
                continue;
            }
            String trimmed = r.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String upper = trimmed.toUpperCase(Locale.ROOT);
            try {
                UserRole.valueOf(upper);
            } catch (IllegalArgumentException ex) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED,
                        "流转角色不合法：" + trimmed);
            }
            result.add(upper);
        }
        return result;
    }

    private void validateWorkflowGraph(List<WorkflowState> states, List<WorkflowTransition> transitions) {
        if (states.isEmpty()) {
            throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "状态定义不能为空");
        }
        Set<String> codes = new HashSet<>();
        int initialCount = 0;
        for (WorkflowState s : states) {
            if (s.getCode() == null || s.getCode().trim().isEmpty()) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "状态编码不能为空");
            }
            if (s.getName() == null || s.getName().trim().isEmpty()) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "状态名称不能为空");
            }
            if (s.getType() == null || s.getType().trim().isEmpty()) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "状态类型不能为空");
            }
            String norm = normalizeStateCode(s.getCode());
            if (!codes.add(norm)) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "状态编码重复：" + s.getCode());
            }
            if (s.isInitial()) {
                initialCount++;
            }
        }
        if (initialCount != 1) {
            throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED,
                    "工作流必须且只能包含一个初始状态（type=INITIAL）");
        }
        if (transitions == null || transitions.isEmpty()) {
            throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "流转规则不能为空");
        }
        for (WorkflowTransition t : transitions) {
            if (t.getFrom() == null || t.getFrom().trim().isEmpty()
                    || t.getTo() == null || t.getTo().trim().isEmpty()) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "流转的起止状态不能为空");
            }
            if (t.getName() == null || t.getName().trim().isEmpty()) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "流转名称不能为空");
            }
            if (t.getId() == null || t.getId().trim().isEmpty()) {
                throw BusinessException.of(ErrorCode.WORKFLOW_VALIDATION_FAILED, "流转规则ID不能为空");
            }
        }
    }

    private String normalizeStateCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toLowerCase(Locale.ROOT);
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
