package com.miduo.cloud.ticket.application.workflow;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketTimeTrackApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowState;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition;
import com.miduo.cloud.ticket.domain.workflow.service.WorkflowEngine;
import com.miduo.cloud.ticket.entity.dto.workflow.AvailableActionOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.ReturnInput;
import com.miduo.cloud.ticket.entity.dto.workflow.TransferInput;
import com.miduo.cloud.ticket.entity.dto.workflow.TransitInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工单工作流应用服务 - 负责工单状态流转、转派、退回
 */
@Service
public class TicketWorkflowAppService extends BaseApplicationService {

    private static final Map<String, String> RETURN_STATUS_MAP = new HashMap<>();

    static {
        RETURN_STATUS_MAP.put("PENDING_VERIFY", "DEVELOPING");
        RETURN_STATUS_MAP.put("PENDING_CS_CONFIRM", "TESTING");
    }

    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkflowEngine workflowEngine;
    private final WorkflowAppService workflowAppService;
    private final TicketTimeTrackApplicationService ticketTimeTrackService;
    private final ApplicationEventPublisher eventPublisher;

    public TicketWorkflowAppService(TicketMapper ticketMapper,
                                     TicketLogMapper ticketLogMapper,
                                     SysUserMapper sysUserMapper,
                                     WorkflowEngine workflowEngine,
                                     WorkflowAppService workflowAppService,
                                     TicketTimeTrackApplicationService ticketTimeTrackService,
                                     ApplicationEventPublisher eventPublisher) {
        this.ticketMapper = ticketMapper;
        this.ticketLogMapper = ticketLogMapper;
        this.sysUserMapper = sysUserMapper;
        this.workflowEngine = workflowEngine;
        this.workflowAppService = workflowAppService;
        this.ticketTimeTrackService = ticketTimeTrackService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 获取工单可用操作列表
     */
    public AvailableActionOutput getAvailableActions(Long ticketId, Long operatorId) {
        TicketPO ticket = getTicketById(ticketId);
        WorkflowPO workflow = workflowAppService.getWorkflowById(ticket.getWorkflowId());

        String userRole = resolveUserRole(operatorId, ticket);

        List<WorkflowTransition> availableTransitions = workflowEngine.getAvailableActions(
                workflow.getStates(), workflow.getTransitions(), ticket.getStatus(), userRole);

        Map<String, String> stateNameMap = buildStateNameMap(workflow.getStates());

        AvailableActionOutput output = new AvailableActionOutput();
        output.setTicketId(ticketId);
        output.setCurrentStatus(ticket.getStatus());
        output.setCurrentStatusName(stateNameMap.getOrDefault(ticket.getStatus(), ticket.getStatus()));

        List<AvailableActionOutput.ActionItem> actions = new ArrayList<>();
        for (WorkflowTransition t : availableTransitions) {
            AvailableActionOutput.ActionItem item = new AvailableActionOutput.ActionItem();
            item.setTargetStatus(t.getTo());
            item.setTargetStatusName(stateNameMap.getOrDefault(t.getTo(), t.getTo()));
            item.setActionName(t.getName());
            item.setAllowedRoles(t.getAllowedRoles());
            actions.add(item);
        }
        output.setActions(actions);

        return output;
    }

    /**
     * 执行状态流转
     */
    @Transactional(rollbackFor = Exception.class)
    public void transit(Long ticketId, TransitInput input, Long operatorId) {
        TicketPO ticket = getTicketById(ticketId);
        WorkflowPO workflow = workflowAppService.getWorkflowById(ticket.getWorkflowId());

        String userRole = resolveUserRole(operatorId, ticket);
        String oldStatus = ticket.getStatus();
        String targetStatus = input.getTargetStatus();
        Long oldAssigneeId = ticket.getAssigneeId();

        boolean valid = workflowEngine.transit(
                workflow.getStates(), workflow.getTransitions(),
                oldStatus, targetStatus, userRole);

        if (!valid) {
            throw BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                    "从[" + oldStatus + "]到[" + targetStatus + "]的流转不合法");
        }

        ticket.setStatus(targetStatus);
        handleTerminalStatus(ticket, targetStatus);
        ticketMapper.updateById(ticket);

        saveTicketLog(ticketId, operatorId, "TRANSIT", oldStatus, targetStatus, input.getRemark());
        String transitionAction = ticketTimeTrackService.resolveTransitionAction(oldStatus, targetStatus);
        ticketTimeTrackService.recordStatusTrack(ticketId, operatorId, transitionAction,
                oldStatus, targetStatus, oldAssigneeId, ticket.getAssigneeId(), input.getRemark());

        eventPublisher.publishEvent(
                new TicketStatusChangedEvent(ticketId, oldStatus, targetStatus, operatorId));
    }

    /**
     * 同角色转派
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long ticketId, TransferInput input, Long operatorId) {
        TicketPO ticket = getTicketById(ticketId);

        SysUserPO targetUser = sysUserMapper.selectById(input.getTargetUserId());
        if (targetUser == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "目标处理人不存在");
        }

        Long previousAssigneeId = ticket.getAssigneeId();

        if (previousAssigneeId != null && !previousAssigneeId.equals(operatorId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "只有当前处理人才能执行转派操作");
        }

        ticket.setAssigneeId(input.getTargetUserId());
        ticketMapper.updateById(ticket);

        saveTicketLog(ticketId, operatorId, "TRANSFER", null, null, input.getReason());
        ticketTimeTrackService.recordTransfer(ticketId, operatorId, previousAssigneeId,
                input.getTargetUserId(), ticket.getStatus(), input.getReason());

        eventPublisher.publishEvent(
                new TicketAssignedEvent(ticketId, input.getTargetUserId(),
                        previousAssigneeId, operatorId, "TRANSFER"));
    }

    /**
     * 退回上一节点
     */
    @Transactional(rollbackFor = Exception.class)
    public void returnTicket(Long ticketId, ReturnInput input, Long operatorId) {
        TicketPO ticket = getTicketById(ticketId);
        String currentStatus = ticket.getStatus();

        String targetStatus = RETURN_STATUS_MAP.get(currentStatus);
        if (targetStatus == null) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID,
                    "当前状态[" + currentStatus + "]不支持退回操作");
        }

        WorkflowPO workflow = workflowAppService.getWorkflowById(ticket.getWorkflowId());
        String userRole = resolveUserRole(operatorId, ticket);

        boolean valid = workflowEngine.validate(
                workflow.getTransitions(), currentStatus, targetStatus, userRole);

        if (!valid) {
            throw BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                    "退回操作校验失败：无权从[" + currentStatus + "]退回到[" + targetStatus + "]");
        }

        String oldStatus = ticket.getStatus();
        Long oldAssigneeId = ticket.getAssigneeId();
        ticket.setStatus(targetStatus);
        ticketMapper.updateById(ticket);

        saveTicketLog(ticketId, operatorId, "RETURN", oldStatus, targetStatus, input.getReason());
        ticketTimeTrackService.recordReturn(ticketId, operatorId, oldStatus, targetStatus,
                oldAssigneeId, ticket.getAssigneeId(), input.getReason());

        eventPublisher.publishEvent(
                new TicketStatusChangedEvent(ticketId, oldStatus, targetStatus, operatorId));
    }

    private TicketPO getTicketById(Long ticketId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    /**
     * 解析操作人在此工单中的角色
     * 根据当前用户与工单的关系，确定其在工作流中的角色
     */
    private String resolveUserRole(Long operatorId, TicketPO ticket) {
        if (operatorId.equals(ticket.getCreatorId())) {
            return "SUBMITTER";
        }
        if (operatorId.equals(ticket.getAssigneeId())) {
            return "HANDLER";
        }
        return "ADMIN";
    }

    private Map<String, String> buildStateNameMap(String statesJson) {
        List<WorkflowState> states = workflowEngine.parseStates(statesJson);
        return states.stream()
                .collect(Collectors.toMap(WorkflowState::getCode, WorkflowState::getName));
    }

    private void handleTerminalStatus(TicketPO ticket, String targetStatus) {
        if ("COMPLETED".equals(targetStatus) || "CLOSED".equals(targetStatus)) {
            ticket.setClosedAt(new Date());
        }
    }

    private void saveTicketLog(Long ticketId, Long operatorId, String action,
                               String oldValue, String newValue, String remark) {
        TicketLogPO logPO = new TicketLogPO();
        logPO.setTicketId(ticketId);
        logPO.setUserId(operatorId);
        logPO.setAction(action);
        logPO.setOldValue(oldValue);
        logPO.setNewValue(newValue);
        logPO.setRemark(remark);
        ticketLogMapper.insert(logPO);
    }
}
