package com.miduo.cloud.ticket.application.workflow;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketAssigneeSyncService;
import com.miduo.cloud.ticket.application.sla.SlaTimerService;
import com.miduo.cloud.ticket.application.ticket.TicketTimeTrackApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCompletedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowState;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition;
import com.miduo.cloud.ticket.domain.workflow.service.WorkflowEngine;
import com.miduo.cloud.ticket.entity.dto.workflow.AvailableActionOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.ReturnInput;
import com.miduo.cloud.ticket.entity.dto.workflow.TicketFlowRecordOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.TransferInput;
import com.miduo.cloud.ticket.entity.dto.workflow.TransitInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketFlowRecordMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketFlowRecordPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单工作流应用服务
 * 所有工单状态流转、转派、退回均通过此服务执行，确保：
 * 1. 状态流转必须经过 WorkflowEngine 校验，无绕过路径
 * 2. 角色基于系统角色 + 工单身份双重判断
 * 3. 每次流转写入 ticket_flow_record 流水，完整追溯
 */
@Service
public class TicketWorkflowAppService extends BaseApplicationService {

    /** 系统自动操作时使用的操作人ID（用于 ticket_log.user_id / ticket_flow_record.operator_id NOT NULL 约束） */
    private static final Long SYSTEM_OPERATOR_ID = 0L;

    /** 流转类型常量 */
    private static final String FLOW_TYPE_TRANSIT  = "TRANSIT";
    private static final String FLOW_TYPE_TRANSFER = "TRANSFER";
    private static final String FLOW_TYPE_RETURN   = "RETURN";
    private static final String FLOW_TYPE_ASSIGN   = "ASSIGN";

    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final TicketFlowRecordMapper flowRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkflowEngine workflowEngine;
    private final WorkflowAppService workflowAppService;
    private final TicketTimeTrackApplicationService ticketTimeTrackService;
    private final SlaTimerService slaTimerService;
    private final ApplicationEventPublisher eventPublisher;
    private final TicketAssigneeSyncService ticketAssigneeSyncService;

    public TicketWorkflowAppService(TicketMapper ticketMapper,
                                     TicketLogMapper ticketLogMapper,
                                     TicketFlowRecordMapper flowRecordMapper,
                                     SysUserMapper sysUserMapper,
                                     WorkflowEngine workflowEngine,
                                     WorkflowAppService workflowAppService,
                                     TicketTimeTrackApplicationService ticketTimeTrackService,
                                     SlaTimerService slaTimerService,
                                     ApplicationEventPublisher eventPublisher,
                                     TicketAssigneeSyncService ticketAssigneeSyncService) {
        this.ticketMapper = ticketMapper;
        this.ticketLogMapper = ticketLogMapper;
        this.flowRecordMapper = flowRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.workflowEngine = workflowEngine;
        this.workflowAppService = workflowAppService;
        this.ticketTimeTrackService = ticketTimeTrackService;
        this.slaTimerService = slaTimerService;
        this.eventPublisher = eventPublisher;
        this.ticketAssigneeSyncService = ticketAssigneeSyncService;
    }

    /**
     * 获取工单可用操作列表（含完整工作流状态，供前端动态渲染）
     * 接口编号：API000014
     */
    public AvailableActionOutput getAvailableActions(Long ticketId, Long operatorId) {
        TicketPO ticket = requireTicket(ticketId);
        WorkflowPO workflow = workflowAppService.getWorkflowById(ticket.getWorkflowId());

        String userRole = resolveUserRole(operatorId, ticket);
        String currentStatus = ticket.getStatus();

        List<WorkflowTransition> availableTransitions = workflowEngine.getAvailableActions(
                workflow.getStates(), workflow.getTransitions(), currentStatus, userRole);

        List<WorkflowState> allStates = workflowEngine.parseStates(workflow.getStates());
        Map<String, String> stateNameMap = allStates.stream()
                .collect(Collectors.toMap(WorkflowState::getCode, WorkflowState::getName));

        AvailableActionOutput output = new AvailableActionOutput();
        output.setTicketId(ticketId);
        output.setCurrentStatus(currentStatus);
        output.setCurrentStatusName(stateNameMap.getOrDefault(currentStatus, currentStatus));

        WorkflowState currentState = workflowEngine.findState(workflow.getStates(), currentStatus);
        output.setIsTerminal(currentState != null && currentState.isTerminal());

        // 构建动作列表
        List<AvailableActionOutput.ActionItem> actions = new ArrayList<>();
        for (WorkflowTransition t : availableTransitions) {
            AvailableActionOutput.ActionItem item = new AvailableActionOutput.ActionItem();
            item.setTransitionId(t.getId());
            item.setTargetStatus(t.getTo());
            item.setTargetStatusName(stateNameMap.getOrDefault(t.getTo(), t.getTo()));
            item.setActionName(t.getName());
            item.setIsReturn(t.isReturnTransition());
            item.setRequireRemark(t.isRequireRemark());
            item.setAllowTransfer(t.isAllowTransfer());
            item.setAllowedRoles(t.getAllowedRoles());
            actions.add(item);
        }
        output.setActions(actions);

        // 构建全状态列表（供进度条/流程图展示）
        List<AvailableActionOutput.StatusItem> statusItems = new ArrayList<>();
        for (WorkflowState s : allStates) {
            AvailableActionOutput.StatusItem statusItem = new AvailableActionOutput.StatusItem();
            statusItem.setCode(s.getCode());
            statusItem.setName(s.getName());
            statusItem.setType(s.getType());
            statusItem.setIsCurrent(s.getCode().equals(currentStatus));
            statusItem.setOrder(s.getOrder());
            statusItems.add(statusItem);
        }
        output.setAllStatuses(statusItems);

        return output;
    }

    /**
     * 执行状态流转（核心方法）
     * 支持 transitionId 精确触发 和 targetStatus 兼容触发两种模式
     * 接口编号：API000015
     */
    @Transactional(rollbackFor = Exception.class)
    public void transit(Long ticketId, TransitInput input, Long operatorId) {
        TicketPO ticket = requireTicket(ticketId);
        WorkflowPO workflow = workflowAppService.getWorkflowById(ticket.getWorkflowId());

        boolean isSystemOperation = (operatorId == null);
        String userRole = isSystemOperation ? "TICKET_ADMIN" : resolveUserRole(operatorId, ticket);
        Long safeOperatorId = isSystemOperation ? SYSTEM_OPERATOR_ID : operatorId;
        if (isSystemOperation) {
            log.info("系统自动流转: ticketId={}, 使用 TICKET_ADMIN 角色, safeOperatorId={}", ticketId, safeOperatorId);
        }

        String oldStatus = ticket.getStatus();
        Long oldAssigneeId = ticket.getAssigneeId();
        List<Long> beforeAssigneeSnapshot = new ArrayList<>(
                ticketAssigneeSyncService.listActiveUserIds(ticketId));

        WorkflowTransition matchedTransition = resolveTransition(
                workflow.getTransitions(), input, oldStatus, userRole);

        String targetStatus = matchedTransition.getTo();

        // 校验 requireRemark
        if (matchedTransition.isRequireRemark()
                && !StringUtils.hasText(input.getRemark())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR,
                    "操作[" + matchedTransition.getName() + "]必须填写备注");
        }

        // 更新工单状态
        ticket.setStatus(targetStatus);

        List<Long> newAssigneeIds = resolveNewAssigneeIdsFromTransit(input);
        List<Long> nextAssigneeIds = Collections.emptyList();
        if (matchedTransition.isAllowTransfer() && !newAssigneeIds.isEmpty()) {
            nextAssigneeIds = newAssigneeIds;
        } else if (shouldAutoClaimOnAccept(matchedTransition, oldStatus, operatorId)) {
            // “受理”动作默认由点击人认领，避免状态已推进但处理人仍停留在旧值
            nextAssigneeIds = Collections.singletonList(operatorId);
        }
        boolean willApplyAssignees = !nextAssigneeIds.isEmpty();
        if (willApplyAssignees) {
            ticketAssigneeSyncService.applyAssigneesToTicket(ticket, nextAssigneeIds);
        }

        // 终态时记录关闭/完成时间
        WorkflowState targetState = workflowEngine.findState(workflow.getStates(), targetStatus);
        if (targetState != null && targetState.isTerminal()) {
            if (TicketStatus.COMPLETED.getCode().equals(targetStatus)) {
                ticket.setResolvedAt(new Date());
            }
            ticket.setClosedAt(new Date());
        }

        ticketMapper.updateById(ticket);

        // 写流转日志（ticket_log 旧格式，保持兼容）
        saveTicketLog(ticketId, safeOperatorId,
                matchedTransition.isReturnTransition() ? "RETURN" : "TRANSIT",
                oldStatus, targetStatus, input.getRemark());

        // 写流转流水（新格式，供审计和分析）
        saveFlowRecord(ticket, matchedTransition,
                matchedTransition.isReturnTransition() ? FLOW_TYPE_RETURN : FLOW_TYPE_TRANSIT,
                oldStatus, targetStatus, oldAssigneeId, ticket.getAssigneeId(),
                safeOperatorId, userRole, input.getRemark());

        // 记录时间追踪
        String transitionAction = ticketTimeTrackService.resolveTransitionAction(oldStatus, targetStatus);
        ticketTimeTrackService.recordStatusTrack(ticketId, safeOperatorId, transitionAction,
                oldStatus, targetStatus, oldAssigneeId, ticket.getAssigneeId(), input.getRemark());

        // SLA计时器联动：根据目标状态的slaAction驱动计时器生命周期
        if (targetState != null) {
            dispatchSlaAction(ticketId, targetState.getSlaAction());
        }

        // 发布领域事件
        eventPublisher.publishEvent(
                new TicketStatusChangedEvent(ticketId, oldStatus, targetStatus, safeOperatorId));

        if (targetState != null && targetState.isTerminal()) {
            eventPublisher.publishEvent(
                    new TicketCompletedEvent(ticketId, targetStatus, safeOperatorId, new Date()));
        }

        // 处理人变更事件（主处理人或协同处理人列表变化时通知）
        if (willApplyAssignees && assigneeUserSetChanged(beforeAssigneeSnapshot, nextAssigneeIds)) {
            String assignmentReason = matchedTransition.isAllowTransfer()
                    ? "TRANSFER_ON_TRANSIT"
                    : "ACCEPT_CLAIM";
            eventPublisher.publishEvent(
                    new TicketAssignedEvent(ticketId, ticket.getAssigneeId(),
                            oldAssigneeId, safeOperatorId, assignmentReason));
        }
    }

    /**
     * 待分派状态下指定处理人：执行工作流中带 allowTransfer 的「分派」流转，同步进入下一状态。
     * 解决仅更新 assigneeId 导致界面仍显示「待分派」的问题。
     * operatorId 为空时按 TICKET_ADMIN 校验流转（自动分派场景）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignFromPendingDispatch(Long ticketId, Long newAssigneeId, String remark, Long operatorId) {
        if (newAssigneeId == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "处理人不能为空");
        }
        assignFromPendingDispatch(ticketId, Collections.singletonList(newAssigneeId), remark, operatorId);
    }

    /**
     * 待分派状态下指定一名或多名处理人并流转到下一节点
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignFromPendingDispatch(Long ticketId, List<Long> newAssigneeIds, String remark, Long operatorId) {
        TicketPO ticket = requireTicket(ticketId);
        if (TicketStatus.fromCode(ticket.getStatus()) != TicketStatus.PENDING_ASSIGN) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID,
                    "当前状态不是待分派，不能使用待分派分派流转");
        }
        if (newAssigneeIds == null || newAssigneeIds.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "请至少指定一名处理人");
        }

        String rawStatus = ticket.getStatus();
        if (rawStatus != null && "pending_dispatch".equalsIgnoreCase(rawStatus.trim())) {
            ticket.setStatus(TicketStatus.PENDING_ASSIGN.getCode());
            ticketMapper.updateById(ticket);
        }

        WorkflowPO workflow = workflowAppService.getWorkflowById(ticket.getWorkflowId());
        String userRole = operatorId == null ? "TICKET_ADMIN" : resolveUserRole(operatorId, ticket);

        WorkflowTransition chosen = findPendingAssignWithTransferTransition(workflow, userRole);
        if (chosen == null) {
            throw BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                    "工作流未配置从待分派携带处理人进入下一节点的流转，请联系管理员");
        }

        TransitInput input = new TransitInput();
        input.setTransitionId(chosen.getId());
        input.setNewAssigneeIds(newAssigneeIds);
        input.setRemark(remark);
        transit(ticketId, input, operatorId);
    }

    /**
     * 从 pending_assign 出发、允许带处理人、且目标非终态的第一条流转（与默认「分派受理/分派测试」一致）
     */
    private WorkflowTransition findPendingAssignWithTransferTransition(WorkflowPO workflow, String userRole) {
        String fromCode = TicketStatus.PENDING_ASSIGN.getCode();
        List<WorkflowTransition> transitions = workflowEngine.parseTransitions(workflow.getTransitions());
        for (WorkflowTransition t : transitions) {
            if (t.getFrom() == null || !fromCode.equalsIgnoreCase(t.getFrom())) {
                continue;
            }
            if (!t.isAllowTransfer()) {
                continue;
            }
            if (t.getFrom() != null && t.getTo() != null
                    && t.getFrom().trim().equalsIgnoreCase(t.getTo().trim())) {
                continue;
            }
            if (!isRoleAllowedForTransition(t, userRole)) {
                continue;
            }
            WorkflowState toState = workflowEngine.findState(workflow.getStates(), t.getTo());
            if (toState != null && toState.isTerminal()) {
                continue;
            }
            return t;
        }
        return null;
    }

    /**
     * 转派（处理人变更，状态不变）
     * 任意已登录用户可操作；流转历史与时间链记录原因
     * 接口编号：API000016
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long ticketId, TransferInput input, Long operatorId) {
        TicketPO ticket = requireTicket(ticketId);

        if (input.getTargetUserId() == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "目标处理人不能为空");
        }

        SysUserPO targetUser = sysUserMapper.selectById(input.getTargetUserId());
        if (targetUser == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "目标处理人不存在");
        }

        String userRole = resolveUserRole(operatorId, ticket);

        Long previousAssigneeId = ticket.getAssigneeId();
        ticketAssigneeSyncService.syncSingleAssigneeRow(ticket, input.getTargetUserId());
        ticketMapper.updateById(ticket);

        saveTicketLog(ticketId, operatorId, "TRANSFER", null, null, input.getReason());

        WorkflowTransition transferTransition = createVirtualTransition(
                "transfer", "转派", ticket.getStatus(), ticket.getStatus());
        saveFlowRecord(ticket, transferTransition,
                FLOW_TYPE_TRANSFER,
                ticket.getStatus(), ticket.getStatus(),
                previousAssigneeId, input.getTargetUserId(),
                operatorId, userRole, input.getReason());

        ticketTimeTrackService.recordTransfer(ticketId, operatorId, previousAssigneeId,
                input.getTargetUserId(), ticket.getStatus(), input.getReason());

        eventPublisher.publishEvent(
                new TicketAssignedEvent(ticketId, input.getTargetUserId(),
                        previousAssigneeId, operatorId, "TRANSFER"));
    }

    /**
     * 退回上一节点（由工作流定义中 isReturn=true 的流转驱动，不再硬编码）
     * 接口编号：API000017
     */
    @Transactional(rollbackFor = Exception.class)
    public void returnTicket(Long ticketId, ReturnInput input, Long operatorId) {
        TicketPO ticket = requireTicket(ticketId);
        WorkflowPO workflow = workflowAppService.getWorkflowById(ticket.getWorkflowId());

        String userRole = resolveUserRole(operatorId, ticket);
        String currentStatus = ticket.getStatus();

        // 从工作流配置中查找退回流转（isReturn=true）
        List<WorkflowTransition> returnTransitions = workflowEngine.getAvailableActions(
                        workflow.getStates(), workflow.getTransitions(), currentStatus, userRole)
                .stream()
                .filter(WorkflowTransition::isReturnTransition)
                .collect(Collectors.toList());

        if (returnTransitions.isEmpty()) {
            String currentStatusLabel = resolveStatusLabel(currentStatus);
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID,
                    "当前状态[" + currentStatusLabel + "]不支持退回操作，或您无权限执行退回");
        }

        // 若有多个退回路径，使用 input.getTargetStatus 指定；否则取第一个
        WorkflowTransition returnTransition;
        if (StringUtils.hasText(input.getTargetStatus())) {
            String targetStatusLabel = resolveStatusLabel(input.getTargetStatus());
            returnTransition = returnTransitions.stream()
                    .filter(t -> t.getTo().equalsIgnoreCase(input.getTargetStatus()))
                    .findFirst()
                    .orElseThrow(() -> BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                            "指定的退回目标状态[" + targetStatusLabel + "]不合法"));
        } else {
            returnTransition = returnTransitions.get(0);
        }

        String targetStatus = returnTransition.getTo();
        String oldStatus = ticket.getStatus();
        Long oldAssigneeId = ticket.getAssigneeId();

        ticket.setStatus(targetStatus);
        ticketMapper.updateById(ticket);

        saveTicketLog(ticketId, operatorId, "RETURN", oldStatus, targetStatus, input.getReason());
        saveFlowRecord(ticket, returnTransition,
                FLOW_TYPE_RETURN,
                oldStatus, targetStatus,
                oldAssigneeId, ticket.getAssigneeId(),
                operatorId, userRole, input.getReason());

        ticketTimeTrackService.recordReturn(ticketId, operatorId, oldStatus, targetStatus,
                oldAssigneeId, ticket.getAssigneeId(), input.getReason());

        eventPublisher.publishEvent(
                new TicketStatusChangedEvent(ticketId, oldStatus, targetStatus, operatorId));
    }

    // =========================================================================
    // 私有辅助方法
    // =========================================================================

    private List<Long> resolveNewAssigneeIdsFromTransit(TransitInput input) {
        List<Long> out = new ArrayList<>();
        if (input.getNewAssigneeIds() != null) {
            for (Long id : input.getNewAssigneeIds()) {
                if (id != null) {
                    out.add(id);
                }
            }
        }
        LinkedHashSet<Long> deduped = new LinkedHashSet<>(out);
        if (!deduped.isEmpty()) {
            return new ArrayList<>(deduped);
        }
        if (input.getNewAssigneeId() != null) {
            return Collections.singletonList(input.getNewAssigneeId());
        }
        return Collections.emptyList();
    }

    private boolean assigneeUserSetChanged(List<Long> before, List<Long> after) {
        Set<Long> b = new HashSet<>(before != null ? before : Collections.emptyList());
        Set<Long> a = new HashSet<>(after != null ? after : Collections.emptyList());
        return !b.equals(a);
    }

    /**
     * 受理动作默认自动认领：
     * - 优先按动作名包含“受理”判断
     * - 兼容历史配置中动作名非“受理”的场景（按 from/to 状态对识别）
     */
    private boolean shouldAutoClaimOnAccept(WorkflowTransition transition, String fromStatus, Long operatorId) {
        if (operatorId == null) {
            return false;
        }
        if (transition != null && StringUtils.hasText(transition.getName())
                && transition.getName().contains("受理")) {
            return true;
        }
        if (!StringUtils.hasText(fromStatus) || transition == null || !StringUtils.hasText(transition.getTo())) {
            return false;
        }
        String normalizedFromStatus = fromStatus.trim().toLowerCase();
        String normalizedToStatus = transition.getTo().trim().toLowerCase();
        return ("pending_accept".equals(normalizedFromStatus) && "processing".equals(normalizedToStatus))
                || ("pending_test_accept".equals(normalizedFromStatus) && "testing".equals(normalizedToStatus))
                || ("pending_dev_accept".equals(normalizedFromStatus) && "developing".equals(normalizedToStatus));
    }

    /**
     * 根据工作流状态的 slaAction 驱动 SLA 计时器生命周期
     * START_RESPONSE / START_RESOLVE → 完成响应计时器（已启动则续跑）
     * PAUSE → 暂停所有 RUNNING 计时器
     * STOP → 完成所有剩余计时器
     */
    private void dispatchSlaAction(Long ticketId, String slaAction) {
        if (slaAction == null) {
            return;
        }
        try {
            switch (slaAction) {
                case "START_RESOLVE":
                    // 完成响应计时器（首次响应），同时恢复暂停中的解决计时器
                    slaTimerService.completeResponseTimer(ticketId);
                    slaTimerService.resumeTimers(ticketId);
                    break;
                case "PAUSE":
                    slaTimerService.pauseTimers(ticketId);
                    break;
                case "STOP":
                    slaTimerService.completeAllTimers(ticketId);
                    break;
                case "START_RESPONSE":
                    slaTimerService.resumeTimers(ticketId);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.warn("SLA计时器联动异常，不影响工作流流转: ticketId={}, slaAction={}, error={}",
                    ticketId, slaAction, e.getMessage());
        }
    }

    /**
     * 解析操作人在此工单中的最高角色
     * 优先级：ADMIN > TICKET_ADMIN > HANDLER（assignee）> SUBMITTER（creator）
     * 同时查询系统角色表，确保角色判断准确
     */
    private String resolveUserRole(Long operatorId, TicketPO ticket) {
        if (operatorId == null) {
            return "SUBMITTER";
        }

        // 查询系统角色（批量已注入）
        List<String> systemRoles = sysUserMapper.selectRoleCodesByUserId(operatorId);

        if (systemRoles != null) {
            if (systemRoles.contains("ADMIN") || systemRoles.contains("admin")) {
                return "ADMIN";
            }
            if (systemRoles.contains("TICKET_ADMIN") || systemRoles.contains("ticket_admin")) {
                return "TICKET_ADMIN";
            }
        }

        // 工单内身份：主处理人或协同处理人
        if (ticket.getId() != null && ticketAssigneeSyncService.isAmongAssignees(ticket.getId(), operatorId)) {
            return "HANDLER";
        }
        if (operatorId.equals(ticket.getCreatorId())) {
            return "SUBMITTER";
        }
        if (systemRoles != null
                && (systemRoles.contains("TESTER") || systemRoles.contains("tester"))) {
            return "TESTER";
        }

        // 其他已登录用户按处理人角色参与流转（环节对全员开放时引擎侧 HANDLER 可匹配）
        return "HANDLER";
    }

    /**
     * 解析本次流转对应的 WorkflowTransition
     * 优先按 transitionId 精确匹配，其次按 targetStatus 匹配
     */
    private WorkflowTransition resolveTransition(String transitionsJson, TransitInput input,
                                                  String currentStatus, String userRole) {
        if (!StringUtils.hasText(input.getTransitionId()) && !StringUtils.hasText(input.getTargetStatus())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "transitionId 和 targetStatus 不能同时为空");
        }

        WorkflowTransition matched;

        if (StringUtils.hasText(input.getTransitionId())) {
            matched = workflowEngine.validateByTransitionId(
                    transitionsJson, input.getTransitionId(), currentStatus, userRole);
            if (matched == null) {
                throw BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                        "流转规则[" + input.getTransitionId() + "]不合法或无权限");
            }
        } else {
            String targetStatus = input.getTargetStatus().toLowerCase();
            boolean valid = workflowEngine.validate(
                    transitionsJson, currentStatus, targetStatus, userRole);
            if (!valid) {
                String currentStatusLabel = resolveStatusLabel(currentStatus);
                String targetStatusLabel = resolveStatusLabel(targetStatus);
                throw BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                        "从[" + currentStatusLabel + "]到[" + targetStatusLabel + "]的流转不合法或无权限");
            }
            // 找到匹配的流转规则（取第一个匹配）
            matched = workflowEngine.parseTransitions(transitionsJson).stream()
                    .filter(t -> t.getFrom().equalsIgnoreCase(currentStatus)
                            && t.getTo().equalsIgnoreCase(targetStatus)
                            && isRoleAllowedForTransition(t, userRole))
                    .findFirst()
                    .orElseThrow(() -> BusinessException.of(ErrorCode.WORKFLOW_TRANSITION_INVALID,
                            "未找到匹配的流转规则"));
        }

        return matched;
    }

    private boolean isRoleAllowedForTransition(WorkflowTransition t, String userRole) {
        if (t.getAllowedRoles() == null || t.getAllowedRoles().isEmpty()) {
            return true;
        }
        return t.getAllowedRoles().contains(userRole);
    }

    /**
     * 将状态码转换为中文状态名，避免前端提示出现 pending_xxx 之类的技术码。
     */
    private String resolveStatusLabel(String statusCode) {
        if (!StringUtils.hasText(statusCode)) {
            return "";
        }
        TicketStatus status = TicketStatus.fromCode(statusCode);
        if (status != null) {
            return status.getLabel();
        }
        return statusCode.trim();
    }

    private TicketPO requireTicket(Long ticketId) {
        if (ticketId == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "工单ID不能为空");
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private void saveTicketLog(Long ticketId, Long operatorId, String action,
                               String oldValue, String newValue, String remark) {
        TicketLogPO logPO = new TicketLogPO();
        logPO.setTicketId(ticketId);
        logPO.setUserId(operatorId != null ? operatorId : SYSTEM_OPERATOR_ID);
        logPO.setAction(action);
        logPO.setOldValue(oldValue);
        logPO.setNewValue(newValue);
        logPO.setRemark(remark);
        ticketLogMapper.insert(logPO);
    }

    private void saveFlowRecord(TicketPO ticket, WorkflowTransition transition,
                                String flowType,
                                String fromStatus, String toStatus,
                                Long fromAssigneeId, Long toAssigneeId,
                                Long operatorId, String operatorRole, String remark) {
        TicketFlowRecordPO record = new TicketFlowRecordPO();
        record.setTicketId(ticket.getId());
        record.setTicketNo(ticket.getTicketNo());
        record.setFlowType(flowType);
        if (transition != null) {
            record.setTransitionId(transition.getId());
            record.setTransitionName(transition.getName());
        }
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setFromAssigneeId(fromAssigneeId);
        record.setToAssigneeId(toAssigneeId);
        record.setOperatorId(operatorId);
        record.setOperatorRole(operatorRole);
        record.setRemark(remark);
        flowRecordMapper.insert(record);
    }

    /**
     * 构造虚拟流转（转派等非状态流转操作用）
     */
    private WorkflowTransition createVirtualTransition(String id, String name,
                                                         String from, String to) {
        WorkflowTransition t = new WorkflowTransition();
        t.setId(id);
        t.setName(name);
        t.setFrom(from);
        t.setTo(to);
        return t;
    }

    /**
     * 查询工单流转历史（时间正序）
     * 接口编号：API000018
     */
    public List<TicketFlowRecordOutput> getFlowHistory(Long ticketId) {
        TicketPO ticket = requireTicket(ticketId);
        WorkflowPO workflow = workflowAppService.getWorkflowById(ticket.getWorkflowId());

        List<TicketFlowRecordPO> records = flowRecordMapper.selectByTicketId(ticketId);
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询相关用户名（批量一次查询，避免 N+1）
        Set<Long> userIds = new HashSet<>();
        for (TicketFlowRecordPO r : records) {
            if (r.getFromAssigneeId() != null) userIds.add(r.getFromAssigneeId());
            if (r.getToAssigneeId() != null)   userIds.add(r.getToAssigneeId());
            if (r.getOperatorId() != null)     userIds.add(r.getOperatorId());
        }
        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUserPO> users = sysUserMapper.selectBatchIds(userIds);
            users.forEach(u -> userNameMap.put(u.getId(), u.getName()));
        }

        // 构建状态名映射
        Map<String, String> stateNameMap = workflowEngine.parseStates(workflow.getStates()).stream()
                .collect(Collectors.toMap(WorkflowState::getCode, WorkflowState::getName));

        List<TicketFlowRecordOutput> result = new ArrayList<>();
        for (TicketFlowRecordPO r : records) {
            TicketFlowRecordOutput output = new TicketFlowRecordOutput();
            output.setId(r.getId());
            output.setTicketId(r.getTicketId());
            output.setTicketNo(r.getTicketNo());
            output.setFlowType(r.getFlowType());
            output.setFlowTypeLabel(resolveFlowTypeLabel(r.getFlowType()));
            output.setTransitionId(r.getTransitionId());
            output.setTransitionName(r.getTransitionName());
            output.setFromStatus(r.getFromStatus());
            output.setFromStatusName(stateNameMap.getOrDefault(r.getFromStatus(), r.getFromStatus()));
            output.setToStatus(r.getToStatus());
            output.setToStatusName(stateNameMap.getOrDefault(r.getToStatus(), r.getToStatus()));
            output.setFromAssigneeId(r.getFromAssigneeId());
            output.setFromAssigneeName(r.getFromAssigneeId() != null
                    ? userNameMap.get(r.getFromAssigneeId()) : null);
            output.setToAssigneeId(r.getToAssigneeId());
            output.setToAssigneeName(r.getToAssigneeId() != null
                    ? userNameMap.get(r.getToAssigneeId()) : null);
            output.setOperatorId(r.getOperatorId());
            output.setOperatorName(r.getOperatorId() != null
                    ? userNameMap.get(r.getOperatorId()) : null);
            output.setOperatorRole(r.getOperatorRole());
            output.setRemark(r.getRemark());
            output.setCreateTime(r.getCreateTime());
            result.add(output);
        }
        return result;
    }

    private String resolveFlowTypeLabel(String flowType) {
        if (flowType == null) {
            return "";
        }
        switch (flowType) {
            case FLOW_TYPE_TRANSIT:  return "状态流转";
            case FLOW_TYPE_TRANSFER: return "转派";
            case FLOW_TYPE_RETURN:   return "退回";
            case FLOW_TYPE_ASSIGN:   return "分派";
            default:                 return flowType;
        }
    }
}
