package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.ticket.TicketAssigneeSyncService;
import com.miduo.cloud.ticket.common.enums.NotificationType;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedAfterAutoDispatchEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketFollowerMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketFollowerPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单领域事件通知监听器
 */
@Component
public class TicketEventNotificationListener {

    private final NotificationOrchestrator notificationOrchestrator;
    private final WecomGroupPushService wecomGroupPushService;
    private final TicketMapper ticketMapper;
    private final TicketFollowerMapper ticketFollowerMapper;
    private final SysUserMapper sysUserMapper;
    private final TicketAssigneeSyncService ticketAssigneeSyncService;

    public TicketEventNotificationListener(NotificationOrchestrator notificationOrchestrator,
                                           WecomGroupPushService wecomGroupPushService,
                                           TicketMapper ticketMapper,
                                           TicketFollowerMapper ticketFollowerMapper,
                                           SysUserMapper sysUserMapper,
                                           TicketAssigneeSyncService ticketAssigneeSyncService) {
        this.notificationOrchestrator = notificationOrchestrator;
        this.wecomGroupPushService = wecomGroupPushService;
        this.ticketMapper = ticketMapper;
        this.ticketFollowerMapper = ticketFollowerMapper;
        this.sysUserMapper = sysUserMapper;
        this.ticketAssigneeSyncService = ticketAssigneeSyncService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCreated(TicketCreatedEvent event) {
        if (event == null) {
            return;
        }
        if (event.isPendingAutoDispatch()) {
            return;
        }
        sendTicketCreatedNotifications(event.getTicketId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCreatedAfterAutoDispatch(TicketCreatedAfterAutoDispatchEvent event) {
        sendTicketCreatedNotifications(event.getTicketId());
    }

    private void sendTicketCreatedNotifications(Long ticketId) {
        if (ticketId == null) {
            return;
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return;
        }

        String creatorName = resolveUserName(ticket.getCreatorId());
        String priorityLabel = resolvePriorityLabel(ticket.getPriority());
        String statusLabel = resolveStatusLabel(ticket.getStatus());
        String title = "新工单待处理 - " + safe(ticket.getTicketNo());
        String content = "工单编号：" + safe(ticket.getTicketNo()) +
                "\n标题：" + safe(ticket.getTitle()) +
                "\n状态：" + statusLabel +
                "\n优先级：" + priorityLabel +
                "\n创建人：" + safe(creatorName);

        List<Long> assignees = collectAssigneeUserIds(ticket);
        for (Long uid : assignees) {
            notificationOrchestrator.dispatch(uid, ticket.getId(), null,
                    NotificationType.TICKET_CREATED, title, content);
        }

        LinkedHashSet<Long> mentionUserIds = new LinkedHashSet<>(assignees);
        if (ticket.getCreatorId() != null) {
            mentionUserIds.add(ticket.getCreatorId());
        }
        wecomGroupPushService.pushByTicketWithUserMentions(ticket.getId(), title, content, mentionUserIds);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketAssigned(TicketAssignedEvent event) {
        TicketPO ticket = ticketMapper.selectById(event.getTicketId());
        if (ticket == null) {
            return;
        }

        List<Long> assignees = collectAssigneeUserIds(ticket);
        if (assignees.isEmpty() && event.getAssigneeId() != null) {
            assignees = Collections.singletonList(event.getAssigneeId());
        }
        if (assignees.isEmpty()) {
            return;
        }

        String operatorName = resolveUserName(event.getOperatorId());
        String creatorName = resolveUserName(ticket.getCreatorId());
        String assigneeNames = resolveAssigneeNames(assignees);
        String priorityLabel = resolvePriorityLabel(ticket.getPriority());
        String statusLabel = resolveStatusLabel(ticket.getStatus());
        String title = "您有新的工单分派 - " + safe(ticket.getTicketNo());
        String content = "工单编号：" + safe(ticket.getTicketNo()) +
                "\n标题：" + safe(ticket.getTitle()) +
                "\n状态：" + statusLabel +
                "\n分派人：" + safe(operatorName) +
                "\n处理人：" + safe(assigneeNames) +
                "\n创建人：" + safe(creatorName) +
                "\n优先级：" + priorityLabel;

        for (Long uid : assignees) {
            notificationOrchestrator.dispatch(uid, ticket.getId(), null,
                    NotificationType.ASSIGNED, title, content);
        }

        LinkedHashSet<Long> mentionUserIds = new LinkedHashSet<>(assignees);
        if (ticket.getCreatorId() != null) {
            mentionUserIds.add(ticket.getCreatorId());
        }
        if (event.getPreviousAssigneeId() != null) {
            mentionUserIds.add(event.getPreviousAssigneeId());
        }
        if (event.getOperatorId() != null) {
            mentionUserIds.add(event.getOperatorId());
        }
        wecomGroupPushService.pushByTicketWithUserMentions(ticket.getId(), title, content, mentionUserIds);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        TicketPO ticket = ticketMapper.selectById(event.getTicketId());
        if (ticket == null) {
            return;
        }

        String oldStatus = resolveStatusLabel(event.getOldStatus());
        String newStatus = resolveStatusLabel(event.getNewStatus());
        String operatorName = resolveUserName(event.getOperatorId());
        String creatorName = resolveUserName(ticket.getCreatorId());
        List<Long> assigneeUserIds = collectAssigneeUserIds(ticket);
        String assigneeNames = resolveAssigneeNames(assigneeUserIds);
        String title = "工单状态更新 - " + safe(ticket.getTicketNo());
        String content = "工单编号：" + safe(ticket.getTicketNo()) +
                "\n标题：" + safe(ticket.getTitle()) +
                "\n状态：" + oldStatus + " → " + newStatus +
                "\n操作人：" + safe(operatorName) +
                "\n创建人：" + safe(creatorName) +
                "\n处理人：" + safe(assigneeNames);

        Set<Long> receiverIds = new LinkedHashSet<>();
        if (ticket.getCreatorId() != null) {
            receiverIds.add(ticket.getCreatorId());
        }
        receiverIds.addAll(assigneeUserIds);

        List<TicketFollowerPO> followers = ticketFollowerMapper.selectList(
                new LambdaQueryWrapper<TicketFollowerPO>()
                        .eq(TicketFollowerPO::getTicketId, ticket.getId())
        );
        if (followers != null && !followers.isEmpty()) {
            receiverIds.addAll(followers.stream()
                    .map(TicketFollowerPO::getUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        if (!receiverIds.isEmpty()) {
            notificationOrchestrator.dispatchToUsers(new ArrayList<>(receiverIds), ticket.getId(), null,
                    NotificationType.STATUS_CHANGED, title, content);
        }

        LinkedHashSet<Long> mentionUserIds = new LinkedHashSet<>(receiverIds);
        if (event.getOperatorId() != null) {
            mentionUserIds.add(event.getOperatorId());
        }
        wecomGroupPushService.pushByTicketWithUserMentions(ticket.getId(), title, content, mentionUserIds);
    }

    private List<Long> collectAssigneeUserIds(TicketPO ticket) {
        List<Long> fromTable = ticketAssigneeSyncService.listActiveUserIds(ticket.getId());
        if (fromTable != null && !fromTable.isEmpty()) {
            return fromTable;
        }
        if (ticket.getAssigneeId() != null) {
            return Collections.singletonList(ticket.getAssigneeId());
        }
        return Collections.emptyList();
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return "-";
        }
        SysUserPO user = sysUserMapper.selectById(userId);
        if (user == null || user.getName() == null) {
            return "-";
        }
        return user.getName();
    }

    private String resolveAssigneeNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return "-";
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(userIds);
        if (users == null || users.isEmpty()) {
            return "-";
        }
        StringBuilder names = new StringBuilder();
        for (SysUserPO user : users) {
            if (user != null && user.getName() != null) {
                if (names.length() > 0) {
                    names.append("、");
                }
                names.append(user.getName());
            }
        }
        return names.length() > 0 ? names.toString() : "-";
    }

    private String resolveStatusLabel(String code) {
        if (code == null) {
            return "-";
        }
        TicketStatus status = TicketStatus.fromCode(code.toLowerCase());
        return status != null ? status.getLabel() : code;
    }

    private String resolvePriorityLabel(String code) {
        if (code == null) {
            return "-";
        }
        Priority priority = Priority.fromCode(code);
        return priority != null ? priority.getLabel() : code;
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
