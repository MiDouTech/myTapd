package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.workflow.TicketWorkflowAppService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.ticket.KanbanColumnOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.KanbanMoveInput;
import com.miduo.cloud.ticket.entity.dto.ticket.KanbanTicketOutput;
import com.miduo.cloud.ticket.entity.dto.workflow.TransitInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工单看板应用服务
 */
@Service
public class KanbanApplicationService extends BaseApplicationService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final SysUserMapper sysUserMapper;
    private final TicketWorkflowAppService ticketWorkflowAppService;

    public KanbanApplicationService(TicketMapper ticketMapper,
                                    TicketCategoryMapper ticketCategoryMapper,
                                    SysUserMapper sysUserMapper,
                                    TicketWorkflowAppService ticketWorkflowAppService) {
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.sysUserMapper = sysUserMapper;
        this.ticketWorkflowAppService = ticketWorkflowAppService;
    }

    public List<KanbanColumnOutput> getKanbanData(Integer limit) {
        int size = normalizeLimit(limit);
        LambdaQueryWrapper<TicketPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TicketPO::getUpdateTime).last("LIMIT " + size);
        List<TicketPO> tickets = ticketMapper.selectList(wrapper);
        if (tickets == null || tickets.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        for (TicketPO ticket : tickets) {
            if (ticket.getAssigneeId() != null) {
                userIds.add(ticket.getAssigneeId());
            }
            if (ticket.getCategoryId() != null) {
                categoryIds.add(ticket.getCategoryId());
            }
        }

        Map<Long, String> userNameMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<SysUserPO> users = sysUserMapper.selectBatchIds(userIds);
            userNameMap = users.stream().collect(Collectors.toMap(SysUserPO::getId, SysUserPO::getName));
        }

        Map<Long, String> categoryNameMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            List<TicketCategoryPO> categories = ticketCategoryMapper.selectBatchIds(categoryIds);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(TicketCategoryPO::getId, TicketCategoryPO::getName));
        }

        Map<String, List<KanbanTicketOutput>> groupedMap = new HashMap<>();
        for (TicketPO ticket : tickets) {
            String status = safeStatus(ticket.getStatus());
            groupedMap.computeIfAbsent(status, key -> new ArrayList<>())
                    .add(convertToKanbanTicket(ticket, userNameMap, categoryNameMap));
        }

        List<String> statusList = new ArrayList<>(groupedMap.keySet());
        statusList.sort(this::compareStatus);

        List<KanbanColumnOutput> outputs = new ArrayList<>();
        for (String status : statusList) {
            KanbanColumnOutput column = new KanbanColumnOutput();
            column.setStatus(status);
            column.setStatusLabel(resolveStatusLabel(status));
            column.setTickets(groupedMap.getOrDefault(status, Collections.emptyList()));
            outputs.add(column);
        }
        return outputs;
    }

    public void moveTicket(KanbanMoveInput input, Long operatorId) {
        if (input == null || input.getTicketId() == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "工单ID不能为空");
        }
        if (input.getTargetStatus() == null || input.getTargetStatus().trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "目标状态不能为空");
        }

        TransitInput transitInput = new TransitInput();
        transitInput.setTargetStatus(input.getTargetStatus().trim());
        transitInput.setRemark(input.getRemark());
        ticketWorkflowAppService.transit(input.getTicketId(), transitInput, operatorId);
    }

    private KanbanTicketOutput convertToKanbanTicket(TicketPO ticket,
                                                     Map<Long, String> userNameMap,
                                                     Map<Long, String> categoryNameMap) {
        KanbanTicketOutput output = new KanbanTicketOutput();
        output.setId(ticket.getId());
        output.setTicketNo(ticket.getTicketNo());
        output.setTitle(ticket.getTitle());
        output.setPriority(ticket.getPriority());
        output.setStatus(safeStatus(ticket.getStatus()));
        output.setStatusLabel(resolveStatusLabel(ticket.getStatus()));
        output.setCategoryName(categoryNameMap.get(ticket.getCategoryId()));
        output.setAssigneeId(ticket.getAssigneeId());
        output.setAssigneeName(userNameMap.get(ticket.getAssigneeId()));
        output.setUpdateTime(ticket.getUpdateTime());

        String priorityCode = ticket.getPriority() == null ? null : ticket.getPriority().toLowerCase(Locale.ROOT);
        Priority priority = Priority.fromCode(priorityCode);
        output.setPriorityLabel(priority != null ? priority.getLabel() : ticket.getPriority());
        return output;
    }

    private int compareStatus(String left, String right) {
        int leftOrder = statusOrder(left);
        int rightOrder = statusOrder(right);
        if (leftOrder != rightOrder) {
            return Integer.compare(leftOrder, rightOrder);
        }
        return safeStatus(left).compareTo(safeStatus(right));
    }

    private int statusOrder(String status) {
        String code = safeStatus(status).toLowerCase(Locale.ROOT);
        if ("pending".equals(code) || "pending_assign".equals(code) || "pending_accept".equals(code)
                || "pending_dispatch".equals(code) || "pending_test".equals(code)
                || "pending_dev".equals(code) || "pending_test_accept".equals(code)
                || "pending_dev_accept".equals(code)) {
            return 1;
        }
        if ("processing".equals(code)
                || "testing".equals(code)
                || "investigating".equals(code)
                || "developing".equals(code)) {
            return 2;
        }
        if ("temp_resolved".equals(code) || "suspended".equals(code)) {
            return 3;
        }
        if ("pending_verify".equals(code) || "pending_cs_confirm".equals(code)) {
            return 4;
        }
        if ("completed".equals(code)) {
            return 5;
        }
        if ("closed".equals(code)) {
            return 6;
        }
        return 99;
    }

    private String resolveStatusLabel(String status) {
        String code = safeStatus(status).toLowerCase(Locale.ROOT);
        TicketStatus ticketStatus = TicketStatus.fromCode(code);
        if (ticketStatus != null) {
            return ticketStatus.getLabel();
        }
        return safeStatus(status);
    }

    private String safeStatus(String status) {
        return status == null ? "" : status;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
