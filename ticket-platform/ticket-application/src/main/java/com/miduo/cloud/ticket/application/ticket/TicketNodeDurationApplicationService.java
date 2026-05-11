package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.domain.common.event.TicketTimeTrackRecordedEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketNodeDurationMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketNodeDurationPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 工单节点耗时统计应用服务
 */
@Service
public class TicketNodeDurationApplicationService extends BaseApplicationService {

    private final TicketNodeDurationMapper nodeDurationMapper;
    private final TicketMapper ticketMapper;
    private final SysUserMapper sysUserMapper;

    public TicketNodeDurationApplicationService(TicketNodeDurationMapper nodeDurationMapper,
                                                TicketMapper ticketMapper,
                                                SysUserMapper sysUserMapper) {
        this.nodeDurationMapper = nodeDurationMapper;
        this.ticketMapper = ticketMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 按时间追踪事件更新节点耗时统计
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleTrackEvent(TicketTimeTrackRecordedEvent event) {
        if (event == null || event.getTicketId() == null) {
            return;
        }
        String action = normalize(event.getAction());
        Date eventTime = event.getTimestamp() != null ? event.getTimestamp() : new Date();

        if ("CREATE".equals(action) || "ESCALATE".equals(action)
                || "RETURN".equals(action) || "COMPLETE".equals(action)) {
            handleStatusTransition(event.getTicketId(), event.getFromStatus(), event.getToStatus(),
                    event.getToUserId(), eventTime);
            return;
        }
        if ("READ".equals(action)) {
            markFirstRead(event.getTicketId(), event.getToStatus(), eventTime, event.getToUserId());
            return;
        }
        if ("START_PROCESS".equals(action)) {
            handleStartProcess(event.getTicketId(), event.getFromStatus(), event.getToStatus(),
                    event.getToUserId(), eventTime);
            return;
        }
        if ("ASSIGN".equals(action) || "TRANSFER".equals(action)) {
            updateCurrentNodeAssignee(event.getTicketId(), event.getToUserId());
        }
    }

    private void handleStartProcess(Long ticketId, String fromStatus, String toStatus,
                                    Long toUserId, Date eventTime) {
        String normalizedFrom = normalize(fromStatus);
        String normalizedTo = normalize(toStatus);
        boolean statusChanged = normalizedFrom != null && normalizedTo != null && !normalizedFrom.equals(normalizedTo);
        if (statusChanged) {
            // 受理动作既代表“前一节点开始处理”，也代表“切换到下一节点”
            markExistingNodeStartProcess(ticketId, normalizedFrom, eventTime);
            handleStatusTransition(ticketId, normalizedFrom, normalizedTo, toUserId, eventTime);
            markStartProcess(ticketId, normalizedTo, eventTime, toUserId);
            return;
        }
        String currentStatus = normalizedTo != null ? normalizedTo : normalizedFrom;
        markStartProcess(ticketId, currentStatus, eventTime, toUserId);
    }

    private void handleStatusTransition(Long ticketId, String fromStatus, String toStatus,
                                        Long toUserId, Date eventTime) {
        String normalizedFrom = normalize(fromStatus);
        String normalizedTo = normalize(toStatus);

        if (normalizedFrom != null && !normalizedFrom.isEmpty()) {
            closeNode(ticketId, normalizedFrom, normalizedTo, eventTime);
        }
        if (normalizedTo != null && !normalizedTo.isEmpty()) {
            openNode(ticketId, normalizedTo, toUserId, eventTime);
        }
    }

    private void markFirstRead(Long ticketId, String status, Date eventTime, Long toUserId) {
        TicketNodeDurationPO node = ensureOpenNode(ticketId, normalize(status), eventTime, toUserId);
        if (node == null) {
            return;
        }
        if (node.getFirstReadAt() == null) {
            node.setFirstReadAt(eventTime);
            if (node.getArriveAt() != null) {
                node.setWaitDurationSec(durationSeconds(node.getFirstReadAt(), node.getArriveAt()));
            }
            nodeDurationMapper.updateById(node);
        }
    }

    private void markStartProcess(Long ticketId, String status, Date eventTime, Long toUserId) {
        TicketNodeDurationPO node = ensureOpenNode(ticketId, normalize(status), eventTime, toUserId);
        if (node == null) {
            return;
        }
        if (node.getStartProcessAt() == null) {
            node.setStartProcessAt(eventTime);
            nodeDurationMapper.updateById(node);
        }
    }

    private void markExistingNodeStartProcess(Long ticketId, String status, Date eventTime) {
        TicketNodeDurationPO node = null;
        if (status != null && !status.isEmpty()) {
            node = getOpenNodeByStatus(ticketId, status);
        }
        if (node == null) {
            node = getOpenNode(ticketId);
        }
        if (node == null || node.getLeaveAt() != null) {
            return;
        }
        if (node.getStartProcessAt() == null) {
            node.setStartProcessAt(eventTime);
            nodeDurationMapper.updateById(node);
        }
    }

    private void updateCurrentNodeAssignee(Long ticketId, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        TicketNodeDurationPO openNode = getOpenNode(ticketId);
        if (openNode == null) {
            TicketPO ticket = ticketMapper.selectById(ticketId);
            if (ticket == null) {
                return;
            }
            openNode = createNode(ticketId, normalize(ticket.getStatus()), assigneeId, new Date());
        }
        openNode.setAssigneeId(assigneeId);
        openNode.setAssigneeRole(resolveAssigneeRole(assigneeId, openNode.getNodeName()));
        nodeDurationMapper.updateById(openNode);
    }

    private TicketNodeDurationPO ensureOpenNode(Long ticketId, String status, Date eventTime, Long toUserId) {
        TicketNodeDurationPO node = null;
        if (status != null && !status.isEmpty()) {
            node = getOpenNodeByStatus(ticketId, status);
        }
        if (node == null) {
            node = getOpenNode(ticketId);
        }
        if (node == null) {
            TicketPO ticket = ticketMapper.selectById(ticketId);
            if (ticket == null) {
                return null;
            }
            String nodeStatus = status;
            if (nodeStatus == null || nodeStatus.isEmpty()) {
                nodeStatus = normalize(ticket.getStatus());
            }
            Long assigneeId = toUserId != null ? toUserId : ticket.getAssigneeId();
            node = createNode(ticketId, nodeStatus, assigneeId, eventTime);
        }
        return node;
    }

    private void closeNode(Long ticketId, String status, String nextStatus, Date leaveAt) {
        TicketNodeDurationPO node = getOpenNodeByStatus(ticketId, status);
        if (node == null) {
            TicketNodeDurationPO openNode = getOpenNode(ticketId);
            if (openNode == null) {
                return;
            }
            String openNodeStatus = normalize(openNode.getNodeName());
            if (nextStatus != null && nextStatus.equals(openNodeStatus)) {
                return;
            }
            log.warn("按状态关闭节点未命中，降级关闭当前开启节点: ticketId={}, expectStatus={}, actualStatus={}",
                    ticketId, status, openNodeStatus);
            node = openNode;
        }
        if (node.getLeaveAt() != null) {
            return;
        }

        node.setLeaveAt(leaveAt);
        if (node.getArriveAt() != null) {
            node.setTotalDurationSec(durationSeconds(leaveAt, node.getArriveAt()));
        }
        if (node.getFirstReadAt() != null && node.getArriveAt() != null) {
            node.setWaitDurationSec(durationSeconds(node.getFirstReadAt(), node.getArriveAt()));
        }
        Date processStart = node.getStartProcessAt();
        if (processStart == null) {
            processStart = node.getFirstReadAt();
            if (processStart != null) {
                node.setStartProcessAt(processStart);
            } else {
                // 没有显式“开始处理”动作时，补齐为离开时刻，保证闭环统计完整
                processStart = leaveAt;
                node.setStartProcessAt(processStart);
            }
        }
        if (processStart != null) {
            node.setProcessDurationSec(durationSeconds(leaveAt, processStart));
        }
        nodeDurationMapper.updateById(node);
    }

    private void openNode(Long ticketId, String status, Long toUserId, Date arriveAt) {
        TicketNodeDurationPO openNode = getOpenNode(ticketId);
        if (openNode != null && status != null && status.equals(normalize(openNode.getNodeName()))) {
            if (toUserId != null && !toUserId.equals(openNode.getAssigneeId())) {
                openNode.setAssigneeId(toUserId);
                openNode.setAssigneeRole(resolveAssigneeRole(toUserId, status));
                nodeDurationMapper.updateById(openNode);
            }
            return;
        }

        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return;
        }
        Long assigneeId = toUserId != null ? toUserId : ticket.getAssigneeId();
        createNode(ticketId, status, assigneeId, arriveAt);
    }

    private TicketNodeDurationPO createNode(Long ticketId, String status, Long assigneeId, Date arriveAt) {
        TicketNodeDurationPO nodePO = new TicketNodeDurationPO();
        nodePO.setTicketId(ticketId);
        nodePO.setNodeName(status);
        nodePO.setAssigneeId(assigneeId);
        nodePO.setAssigneeRole(resolveAssigneeRole(assigneeId, status));
        nodePO.setArriveAt(arriveAt != null ? arriveAt : new Date());
        nodeDurationMapper.insert(nodePO);
        return nodePO;
    }

    private TicketNodeDurationPO getOpenNodeByStatus(Long ticketId, String status) {
        return nodeDurationMapper.selectOne(new LambdaQueryWrapper<TicketNodeDurationPO>()
                .eq(TicketNodeDurationPO::getTicketId, ticketId)
                .eq(TicketNodeDurationPO::getNodeName, status)
                .isNull(TicketNodeDurationPO::getLeaveAt)
                .orderByDesc(TicketNodeDurationPO::getArriveAt)
                .orderByDesc(TicketNodeDurationPO::getId)
                .last("LIMIT 1"));
    }

    private TicketNodeDurationPO getOpenNode(Long ticketId) {
        return nodeDurationMapper.selectOne(new LambdaQueryWrapper<TicketNodeDurationPO>()
                .eq(TicketNodeDurationPO::getTicketId, ticketId)
                .isNull(TicketNodeDurationPO::getLeaveAt)
                .orderByDesc(TicketNodeDurationPO::getArriveAt)
                .orderByDesc(TicketNodeDurationPO::getId)
                .last("LIMIT 1"));
    }

    private Long durationSeconds(Date end, Date start) {
        if (end == null || start == null) {
            return null;
        }
        long seconds = (end.getTime() - start.getTime()) / 1000;
        return Math.max(seconds, 0L);
    }

    private String resolveAssigneeRole(Long assigneeId, String status) {
        if (assigneeId == null) {
            return null;
        }
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(assigneeId);
        Set<String> normalizedRoles = new HashSet<>();
        if (roleCodes != null) {
            for (String roleCode : roleCodes) {
                if (roleCode != null) {
                    normalizedRoles.add(normalize(roleCode));
                }
            }
        }
        if (normalizedRoles.contains("ADMIN") || normalizedRoles.contains("TICKET_ADMIN")) {
            return "SYSTEM";
        }
        if (normalizedRoles.contains("CUSTOMER_SERVICE") || normalizedRoles.contains("SUBMITTER")) {
            return "CUSTOMER_SERVICE";
        }
        if (normalizedRoles.contains("TESTER")) {
            return "TESTER";
        }
        if (normalizedRoles.contains("DEVELOPER")) {
            return "DEVELOPER";
        }
        return inferRoleByStatus(status);
    }

    private String inferRoleByStatus(String status) {
        String normalizedStatus = normalize(status);
        if (normalizedStatus == null) {
            return "SYSTEM";
        }
        if (normalizedStatus.contains("DEV")) {
            return "DEVELOPER";
        }
        if (normalizedStatus.contains("TEST") || normalizedStatus.contains("VERIFY")) {
            return "TESTER";
        }
        if (normalizedStatus.contains("CS") || normalizedStatus.contains("DISPATCH")) {
            return "CUSTOMER_SERVICE";
        }
        return "SYSTEM";
    }

    private String normalize(String text) {
        return text == null ? null : text.trim().toUpperCase(Locale.ROOT);
    }
}
