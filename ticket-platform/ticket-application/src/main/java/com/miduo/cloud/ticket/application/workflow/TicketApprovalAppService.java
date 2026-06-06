package com.miduo.cloud.ticket.application.workflow;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.miduo.cloud.ticket.common.enums.ApprovalActionType;
import com.miduo.cloud.ticket.common.enums.ApprovalMode;
import com.miduo.cloud.ticket.common.enums.ApprovalTaskStatus;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.workflow.model.WorkflowTransition;
import com.miduo.cloud.ticket.entity.dto.workflow.ApprovalActionInput;
import com.miduo.cloud.ticket.entity.dto.workflow.ApprovalPendingListOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.ApprovalTaskOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.TransitInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.HandlerGroupMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.TicketApprovalRecordMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.TicketApprovalTaskMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.WorkflowMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.HandlerGroupPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.TicketApprovalRecordPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.TicketApprovalTaskPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工单审批引擎应用服务
 * 嵌入式审批任务层，借鉴米多星球 WorkflowFlowInstanceServiceImpl 核心逻辑，
 * 适配工单系统的 FSM 工作流体系。
 *
 * 设计原则：
 * 1. FSM 负责宏观状态管理（submitted → dept_approval → executing）
 * 2. 本服务负责 dept_approval 状态内的微观审批任务管理
 * 3. 审批任务全部通过后，自动触发 FSM 下一个 transition（system 操作身份）
 * 4. 被驳回时，自动触发 FSM 驳回 transition
 */
@Service
public class TicketApprovalAppService {

    private static final Logger log = LoggerFactory.getLogger(TicketApprovalAppService.class);

    private final TicketApprovalTaskMapper approvalTaskMapper;
    private final TicketApprovalRecordMapper approvalRecordMapper;
    private final TicketMapper ticketMapper;
    private final SysUserMapper sysUserMapper;
    private final HandlerGroupMapper handlerGroupMapper;
    private final WorkflowMapper workflowMapper;
    private final TicketWorkflowAppService ticketWorkflowAppService;

    public TicketApprovalAppService(TicketApprovalTaskMapper approvalTaskMapper,
                                    TicketApprovalRecordMapper approvalRecordMapper,
                                    TicketMapper ticketMapper,
                                    SysUserMapper sysUserMapper,
                                    HandlerGroupMapper handlerGroupMapper,
                                    WorkflowMapper workflowMapper,
                                    TicketWorkflowAppService ticketWorkflowAppService) {
        this.approvalTaskMapper = approvalTaskMapper;
        this.approvalRecordMapper = approvalRecordMapper;
        this.ticketMapper = ticketMapper;
        this.sysUserMapper = sysUserMapper;
        this.handlerGroupMapper = handlerGroupMapper;
        this.workflowMapper = workflowMapper;
        this.ticketWorkflowAppService = ticketWorkflowAppService;
    }

    /**
     * 启动审批任务（FSM 流转到审批状态后调用）
     * 解析 transition.approvalConfig，创建审批任务
     *
     * @param ticketId    工单ID
     * @param transition  触发审批的工作流流转规则（含 approvalConfig）
     * @param initiatorId 发起人（提交审批的用户ID）
     */
    @Transactional(rollbackFor = Exception.class)
    public void startApproval(Long ticketId, WorkflowTransition transition, Long initiatorId) {
        if (ticketId == null || transition == null || !transition.needsApproval()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "审批配置异常，无法启动审批");
        }
        WorkflowTransition.ApprovalConfig config = transition.getApprovalConfig();
        if (config == null || config.getNodes() == null || config.getNodes().isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "审批流配置为空，无法启动审批");
        }

        long existingActive = approvalTaskMapper.countActiveByTicketAndTransition(ticketId, transition.getId());
        if (existingActive > 0) {
            log.warn("审批任务已存在，跳过重复创建: ticketId={}, transitionId={}", ticketId, transition.getId());
            return;
        }

        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "工单不存在");
        }

        List<WorkflowTransition.ApprovalNode> nodes = config.getNodes();
        Date now = new Date();

        for (WorkflowTransition.ApprovalNode node : nodes) {
            List<Long> assigneeIds = resolveAssignees(node, ticket);

            if (assigneeIds.isEmpty()) {
                log.warn("审批节点未能解析到审批人，跳过: ticketId={}, nodeKey={}, assigneeType={}",
                        ticketId, node.getNodeKey(), node.getAssigneeType());
                continue;
            }

            ApprovalMode mode = ApprovalMode.fromCode(node.getApproveMode());

            for (int i = 0; i < assigneeIds.size(); i++) {
                Long assigneeId = assigneeIds.get(i);
                SysUserPO assignee = sysUserMapper.selectById(assigneeId);
                String assigneeName = assignee != null && StringUtils.hasText(assignee.getName())
                        ? assignee.getName() : String.valueOf(assigneeId);

                TicketApprovalTaskPO task = new TicketApprovalTaskPO();
                task.setTicketId(ticketId);
                task.setTransitionId(transition.getId());
                task.setNodeKey(node.getNodeKey());
                task.setNodeName(StringUtils.hasText(node.getNodeName()) ? node.getNodeName() : "审批");
                task.setApproveMode(mode.getCode());
                task.setAssigneeId(assigneeId);
                task.setAssigneeName(assigneeName);
                task.setSortOrder(i);

                if (mode == ApprovalMode.SEQUENTIAL && i > 0) {
                    task.setTaskStatus(ApprovalTaskStatus.WAITING.getCode());
                } else {
                    task.setTaskStatus(ApprovalTaskStatus.PENDING.getCode());
                }

                if (node.getDueHours() != null && node.getDueHours() > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(now);
                    cal.add(Calendar.HOUR_OF_DAY, node.getDueHours());
                    task.setDueTime(cal.getTime());
                }

                approvalTaskMapper.insert(task);
            }
        }

        log.info("审批任务创建完成: ticketId={}, transitionId={}, initiatorId={}", ticketId, transition.getId(), initiatorId);
    }

    /**
     * 执行审批操作（同意 / 驳回 / 转交）
     * 借鉴米多星球 performApproveFlowInternal 核心逻辑
     *
     * @param input      审批操作请求
     * @param operatorId 操作人用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void performApprove(ApprovalActionInput input, Long operatorId) {
        if (input == null || input.getTaskId() == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "审批任务ID不能为空");
        }

        TicketApprovalTaskPO task = approvalTaskMapper.selectById(input.getTaskId());
        if (task == null || (task.getDeleted() != null && task.getDeleted() == 1)) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "审批任务不存在");
        }
        if (!ApprovalTaskStatus.PENDING.getCode().equals(task.getTaskStatus())) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "当前审批任务状态不允许操作（状态：" + task.getTaskStatus() + "）");
        }
        if (!task.getAssigneeId().equals(operatorId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "您不是该审批任务的指定审批人，无权操作");
        }

        ApprovalActionType action = ApprovalActionType.fromCode(input.getActionType());
        if (action == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "不支持的操作类型：" + input.getActionType());
        }

        TicketPO ticket = ticketMapper.selectById(task.getTicketId());
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "工单不存在");
        }

        SysUserPO operator = sysUserMapper.selectById(operatorId);
        String operatorName = operator != null && StringUtils.hasText(operator.getName())
                ? operator.getName() : String.valueOf(operatorId);
        Date now = new Date();

        switch (action) {
            case APPROVE:
                doApprove(task, ticket, operatorId, operatorName, input.getRemark(), now);
                break;
            case REJECT:
                doReject(task, ticket, operatorId, operatorName, input.getRemark(), now);
                break;
            case TRANSFER:
                doTransfer(task, ticket, operatorId, operatorName, input.getRemark(), input.getTargetAssigneeId(), now);
                break;
            default:
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "不支持的操作类型：" + action.getCode());
        }
    }

    /**
     * 查询工单审批任务详情（含审批时间轴）
     * 接口编号：API000517
     */
    public ApprovalTaskOutput getApprovalTasks(Long ticketId, Long currentUserId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "工单不存在");
        }

        List<TicketApprovalTaskPO> allTasks = approvalTaskMapper.selectByTicketId(ticketId);
        List<TicketApprovalRecordPO> allRecords = approvalRecordMapper.selectByTicketId(ticketId);

        ApprovalTaskOutput output = new ApprovalTaskOutput();
        output.setTicketId(ticketId);

        Map<String, List<TicketApprovalTaskPO>> byNode = new LinkedHashMap<>();
        for (TicketApprovalTaskPO t : allTasks) {
            byNode.computeIfAbsent(t.getNodeKey(), k -> new ArrayList<>()).add(t);
        }

        List<ApprovalTaskOutput.ApprovalNodeGroup> nodeGroups = new ArrayList<>();
        for (Map.Entry<String, List<TicketApprovalTaskPO>> entry : byNode.entrySet()) {
            ApprovalTaskOutput.ApprovalNodeGroup group = new ApprovalTaskOutput.ApprovalNodeGroup();
            group.setNodeKey(entry.getKey());
            List<TicketApprovalTaskPO> tasksInNode = entry.getValue();
            group.setNodeName(tasksInNode.get(0).getNodeName());
            group.setApproveMode(tasksInNode.get(0).getApproveMode());

            List<ApprovalTaskOutput.TaskItem> taskItems = new ArrayList<>();
            for (TicketApprovalTaskPO t : tasksInNode) {
                ApprovalTaskOutput.TaskItem item = new ApprovalTaskOutput.TaskItem();
                item.setTaskId(t.getId());
                item.setAssigneeId(t.getAssigneeId());
                item.setAssigneeName(t.getAssigneeName());
                item.setTaskStatus(t.getTaskStatus());
                item.setTaskStatusLabel(resolveTaskStatusLabel(t.getTaskStatus()));
                item.setRemark(t.getRemark());
                item.setOperateTime(t.getOperateTime());
                item.setDueTime(t.getDueTime());
                item.setSortOrder(t.getSortOrder());
                taskItems.add(item);
            }
            group.setTasks(taskItems);
            nodeGroups.add(group);
        }
        output.setNodes(nodeGroups);

        Map<String, String> nodeNameMap = new HashMap<>();
        for (TicketApprovalTaskPO t : allTasks) {
            nodeNameMap.put(t.getNodeKey(), t.getNodeName());
        }

        List<ApprovalTaskOutput.ApprovalRecordItem> recordItems = new ArrayList<>();
        for (TicketApprovalRecordPO r : allRecords) {
            ApprovalTaskOutput.ApprovalRecordItem item = new ApprovalTaskOutput.ApprovalRecordItem();
            item.setRecordId(r.getId());
            item.setTaskId(r.getTaskId());
            item.setNodeKey(r.getNodeKey());
            item.setNodeName(nodeNameMap.getOrDefault(r.getNodeKey(), r.getNodeKey()));
            item.setActionType(r.getActionType());
            item.setActionLabel(resolveActionLabel(r.getActionType()));
            item.setOperatorId(r.getOperatorId());
            item.setOperatorName(r.getOperatorName());
            item.setRemark(r.getRemark());
            item.setTargetAssigneeId(r.getTargetAssigneeId());
            item.setTargetAssigneeName(r.getTargetAssigneeName());
            item.setCreateTime(r.getCreateTime());
            recordItems.add(item);
        }
        output.setRecords(recordItems);

        boolean hasPending = allTasks.stream()
                .anyMatch(t -> ApprovalTaskStatus.PENDING.getCode().equals(t.getTaskStatus()));
        output.setHasPendingTask(hasPending);

        Long myPendingTaskId = allTasks.stream()
                .filter(t -> ApprovalTaskStatus.PENDING.getCode().equals(t.getTaskStatus())
                        && t.getAssigneeId().equals(currentUserId))
                .map(TicketApprovalTaskPO::getId)
                .findFirst().orElse(null);
        output.setMyPendingTaskId(myPendingTaskId);

        return output;
    }

    /**
     * 查询当前用户待审批任务数量（导航角标）
     * 接口编号：API000518
     */
    public long getPendingCount(Long userId) {
        return approvalTaskMapper.countPendingByAssignee(userId);
    }

    /**
     * 查询当前用户待审批任务列表
     * 接口编号：API000519
     */
    public ApprovalPendingListOutput getPendingList(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<TicketApprovalTaskPO> tasks = approvalTaskMapper.selectPendingPageByAssignee(userId, offset, pageSize);
        long total = approvalTaskMapper.countPendingByAssignee(userId);

        List<Long> ticketIds = tasks.stream()
                .map(TicketApprovalTaskPO::getTicketId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, TicketPO> ticketMap = new HashMap<>();
        if (!ticketIds.isEmpty()) {
            ticketMapper.selectBatchIds(ticketIds).forEach(t -> ticketMap.put(t.getId(), t));
        }

        Date now = new Date();
        List<ApprovalPendingListOutput.PendingItem> items = new ArrayList<>();
        for (TicketApprovalTaskPO task : tasks) {
            TicketPO ticket = ticketMap.get(task.getTicketId());
            ApprovalPendingListOutput.PendingItem item = new ApprovalPendingListOutput.PendingItem();
            item.setTaskId(task.getId());
            item.setTicketId(task.getTicketId());
            item.setTicketNo(ticket != null ? ticket.getTicketNo() : null);
            item.setTicketTitle(ticket != null ? ticket.getTitle() : null);
            item.setTicketStatus(ticket != null ? ticket.getStatus() : null);
            item.setNodeName(task.getNodeName());
            item.setApproveMode(task.getApproveMode());
            item.setCreateTime(task.getCreateTime());
            item.setDueTime(task.getDueTime());
            if (task.getCreateTime() != null) {
                item.setWaitMinutes((now.getTime() - task.getCreateTime().getTime()) / 60000L);
            }
            item.setIsOverdue(task.getDueTime() != null && now.after(task.getDueTime()));
            items.add(item);
        }

        ApprovalPendingListOutput output = new ApprovalPendingListOutput();
        output.setTotalCount(total);
        output.setItems(items);
        return output;
    }

    // =========================================================
    // 私有：审批核心逻辑（同意/驳回/转交）
    // =========================================================

    private void doApprove(TicketApprovalTaskPO task, TicketPO ticket,
                           Long operatorId, String operatorName, String remark, Date now) {
        finishTask(task, ApprovalTaskStatus.APPROVED, remark, now);
        insertRecord(task, ApprovalActionType.APPROVE.getCode(), operatorId, operatorName, remark, null, null);

        ApprovalMode mode = ApprovalMode.fromCode(task.getApproveMode());

        if (mode == ApprovalMode.COUNTERSIGN) {
            long pending = approvalTaskMapper.countPendingByNodeKey(task.getTicketId(), task.getNodeKey());
            if (pending > 0) {
                log.info("会签节点仍有{}个待审批任务: ticketId={}, nodeKey={}", pending, task.getTicketId(), task.getNodeKey());
                return;
            }
        } else if (mode == ApprovalMode.ORSIGN) {
            skipOtherPendingInNode(task.getTicketId(), task.getNodeKey(), task.getId());
        } else if (mode == ApprovalMode.SEQUENTIAL) {
            TicketApprovalTaskPO nextWaiting = approvalTaskMapper.selectNextWaitingByNodeKey(
                    task.getTicketId(), task.getNodeKey());
            if (nextWaiting != null) {
                nextWaiting.setTaskStatus(ApprovalTaskStatus.PENDING.getCode());
                approvalTaskMapper.updateById(nextWaiting);
                log.info("依次审批：激活下一个审批人: taskId={}, assigneeId={}", nextWaiting.getId(), nextWaiting.getAssigneeId());
                return;
            }
        }

        proceedToNextNodeOrComplete(task, ticket);
    }

    private void doReject(TicketApprovalTaskPO task, TicketPO ticket,
                          Long operatorId, String operatorName, String remark, Date now) {
        finishTask(task, ApprovalTaskStatus.REJECTED, remark, now);
        insertRecord(task, ApprovalActionType.REJECT.getCode(), operatorId, operatorName, remark, null, null);

        skipAllPendingForTicketTransition(task.getTicketId(), task.getTransitionId(), task.getId());

        WorkflowTransition transition = findTransition(ticket.getWorkflowId(), task.getTransitionId());
        if (transition != null && transition.getApprovalConfig() != null) {
            String rejectedStatus = transition.getApprovalConfig().getRejectedStatus();
            if (StringUtils.hasText(rejectedStatus)) {
                TransitInput transitInput = new TransitInput();
                transitInput.setTargetStatus(rejectedStatus);
                transitInput.setRemark(remark);
                ticketWorkflowAppService.transit(ticket.getId(), transitInput, null);
                log.info("审批被驳回，工单自动流转: ticketId={}, rejectedStatus={}", ticket.getId(), rejectedStatus);
            }
        }
    }

    private void doTransfer(TicketApprovalTaskPO task, TicketPO ticket,
                            Long operatorId, String operatorName, String remark,
                            Long targetAssigneeId, Date now) {
        if (targetAssigneeId == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "转交目标人不能为空");
        }
        SysUserPO targetUser = sysUserMapper.selectById(targetAssigneeId);
        if (targetUser == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "转交目标用户不存在");
        }
        String targetName = StringUtils.hasText(targetUser.getName()) ? targetUser.getName() : String.valueOf(targetAssigneeId);

        finishTask(task, ApprovalTaskStatus.TRANSFERRED, remark, now);
        insertRecord(task, ApprovalActionType.TRANSFER.getCode(), operatorId, operatorName, remark, targetAssigneeId, targetName);

        TicketApprovalTaskPO newTask = new TicketApprovalTaskPO();
        newTask.setTicketId(task.getTicketId());
        newTask.setTransitionId(task.getTransitionId());
        newTask.setNodeKey(task.getNodeKey());
        newTask.setNodeName(task.getNodeName());
        newTask.setApproveMode(task.getApproveMode());
        newTask.setAssigneeId(targetAssigneeId);
        newTask.setAssigneeName(targetName);
        newTask.setTaskStatus(ApprovalTaskStatus.PENDING.getCode());
        newTask.setSortOrder(task.getSortOrder());
        newTask.setDueTime(task.getDueTime());
        approvalTaskMapper.insert(newTask);

        log.info("审批任务已转交: ticketId={}, oldTaskId={}, newTaskId={}, targetAssigneeId={}",
                task.getTicketId(), task.getId(), newTask.getId(), targetAssigneeId);
    }

    private void proceedToNextNodeOrComplete(TicketApprovalTaskPO currentTask, TicketPO ticket) {
        List<TicketApprovalTaskPO> allTasks = approvalTaskMapper.selectByTicketId(ticket.getId());
        boolean hasMoreActive = allTasks.stream()
                .filter(t -> !t.getId().equals(currentTask.getId()))
                .anyMatch(t -> ApprovalTaskStatus.PENDING.getCode().equals(t.getTaskStatus())
                        || ApprovalTaskStatus.WAITING.getCode().equals(t.getTaskStatus()));

        if (hasMoreActive) {
            log.info("当前节点完成，后续节点仍有待处理任务: ticketId={}", ticket.getId());
            return;
        }

        WorkflowTransition transition = findTransition(ticket.getWorkflowId(), currentTask.getTransitionId());
        if (transition != null && transition.getApprovalConfig() != null) {
            String passedStatus = transition.getApprovalConfig().getPassedStatus();
            if (StringUtils.hasText(passedStatus)) {
                TransitInput transitInput = new TransitInput();
                transitInput.setTargetStatus(passedStatus);
                transitInput.setRemark("审批通过，自动流转");
                ticketWorkflowAppService.transit(ticket.getId(), transitInput, null);
                log.info("审批全部通过，工单自动流转: ticketId={}, passedStatus={}", ticket.getId(), passedStatus);
            }
        }
    }

    private void finishTask(TicketApprovalTaskPO task, ApprovalTaskStatus status, String remark, Date now) {
        task.setTaskStatus(status.getCode());
        task.setRemark(remark);
        task.setOperateTime(now);
        approvalTaskMapper.updateById(task);
    }

    private void insertRecord(TicketApprovalTaskPO task, String actionType,
                              Long operatorId, String operatorName, String remark,
                              Long targetAssigneeId, String targetAssigneeName) {
        TicketApprovalRecordPO record = new TicketApprovalRecordPO();
        record.setTicketId(task.getTicketId());
        record.setTaskId(task.getId());
        record.setNodeKey(task.getNodeKey());
        record.setActionType(actionType);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setRemark(remark);
        record.setTargetAssigneeId(targetAssigneeId);
        record.setTargetAssigneeName(targetAssigneeName);
        approvalRecordMapper.insert(record);
    }

    private void skipOtherPendingInNode(Long ticketId, String nodeKey, Long excludeTaskId) {
        List<TicketApprovalTaskPO> others = approvalTaskMapper.selectList(
                new LambdaQueryWrapper<TicketApprovalTaskPO>()
                        .eq(TicketApprovalTaskPO::getTicketId, ticketId)
                        .eq(TicketApprovalTaskPO::getNodeKey, nodeKey)
                        .eq(TicketApprovalTaskPO::getTaskStatus, ApprovalTaskStatus.PENDING.getCode())
                        .ne(TicketApprovalTaskPO::getId, excludeTaskId)
                        .eq(TicketApprovalTaskPO::getDeleted, 0));
        Date now = new Date();
        for (TicketApprovalTaskPO t : others) {
            approvalTaskMapper.update(null,
                    new LambdaUpdateWrapper<TicketApprovalTaskPO>()
                            .set(TicketApprovalTaskPO::getTaskStatus, ApprovalTaskStatus.SKIPPED.getCode())
                            .set(TicketApprovalTaskPO::getOperateTime, now)
                            .eq(TicketApprovalTaskPO::getId, t.getId()));
        }
    }

    private void skipAllPendingForTicketTransition(Long ticketId, String transitionId, Long excludeTaskId) {
        List<TicketApprovalTaskPO> pending = approvalTaskMapper.selectList(
                new LambdaQueryWrapper<TicketApprovalTaskPO>()
                        .eq(TicketApprovalTaskPO::getTicketId, ticketId)
                        .eq(TicketApprovalTaskPO::getTransitionId, transitionId)
                        .in(TicketApprovalTaskPO::getTaskStatus,
                                ApprovalTaskStatus.PENDING.getCode(), ApprovalTaskStatus.WAITING.getCode())
                        .ne(TicketApprovalTaskPO::getId, excludeTaskId)
                        .eq(TicketApprovalTaskPO::getDeleted, 0));
        Date now = new Date();
        for (TicketApprovalTaskPO t : pending) {
            approvalTaskMapper.update(null,
                    new LambdaUpdateWrapper<TicketApprovalTaskPO>()
                            .set(TicketApprovalTaskPO::getTaskStatus, ApprovalTaskStatus.SKIPPED.getCode())
                            .set(TicketApprovalTaskPO::getOperateTime, now)
                            .eq(TicketApprovalTaskPO::getId, t.getId()));
        }
    }

    // =========================================================
    // 私有：审批人解析
    // =========================================================

    private List<Long> resolveAssignees(WorkflowTransition.ApprovalNode node, TicketPO ticket) {
        String assigneeType = node.getAssigneeType();
        if ("member".equals(assigneeType)) {
            return node.getAssigneeIds() != null ? node.getAssigneeIds() : new ArrayList<>();
        }
        if ("groupLeader".equals(assigneeType)) {
            return resolveGroupLeaderAssignees(ticket);
        }
        return new ArrayList<>();
    }

    private List<Long> resolveGroupLeaderAssignees(TicketPO ticket) {
        List<Long> result = new ArrayList<>();
        List<HandlerGroupPO> groups = handlerGroupMapper.selectList(
                new LambdaQueryWrapper<HandlerGroupPO>()
                        .eq(HandlerGroupPO::getIsActive, 1)
                        .eq(HandlerGroupPO::getDeleted, 0)
                        .isNotNull(HandlerGroupPO::getLeaderId));
        if (!groups.isEmpty()) {
            groups.stream()
                    .filter(g -> g.getLeaderId() != null)
                    .findFirst()
                    .ifPresent(g -> result.add(g.getLeaderId()));
        }
        if (result.isEmpty() && ticket.getAssigneeId() != null) {
            result.add(ticket.getAssigneeId());
        }
        return result;
    }

    // =========================================================
    // 私有：从 workflow.transitions JSON 读取 WorkflowTransition
    // =========================================================

    private WorkflowTransition findTransition(Long workflowId, String transitionId) {
        if (workflowId == null || !StringUtils.hasText(transitionId)) {
            return null;
        }
        WorkflowPO workflow = workflowMapper.selectOne(
                new LambdaQueryWrapper<WorkflowPO>()
                        .eq(WorkflowPO::getId, workflowId)
                        .eq(WorkflowPO::getDeleted, 0));
        if (workflow == null || !StringUtils.hasText(workflow.getTransitions())) {
            return null;
        }
        try {
            List<WorkflowTransition> transitions = JSON.parseArray(workflow.getTransitions(), WorkflowTransition.class);
            if (transitions == null) {
                return null;
            }
            return transitions.stream()
                    .filter(t -> transitionId.equals(t.getId()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("解析工作流 transitions JSON 失败: workflowId={}", workflowId, e);
            return null;
        }
    }

    // =========================================================
    // 私有：标签解析工具
    // =========================================================

    private String resolveTaskStatusLabel(String code) {
        ApprovalTaskStatus s = ApprovalTaskStatus.fromCode(code);
        return s != null ? s.getLabel() : code;
    }

    private String resolveActionLabel(String code) {
        ApprovalActionType a = ApprovalActionType.fromCode(code);
        return a != null ? a.getLabel() : code;
    }
}
