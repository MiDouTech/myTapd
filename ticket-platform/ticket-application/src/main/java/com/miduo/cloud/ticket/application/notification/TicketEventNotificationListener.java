package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.NotificationType;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketFollowerMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketFollowerPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工单领域事件通知监听器
 */
@Component
public class TicketEventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(TicketEventNotificationListener.class);

    private final NotificationOrchestrator notificationOrchestrator;
    private final WecomGroupPushService wecomGroupPushService;
    private final TicketMapper ticketMapper;
    private final TicketFollowerMapper ticketFollowerMapper;
    private final SysUserMapper sysUserMapper;

    public TicketEventNotificationListener(NotificationOrchestrator notificationOrchestrator,
                                           WecomGroupPushService wecomGroupPushService,
                                           TicketMapper ticketMapper,
                                           TicketFollowerMapper ticketFollowerMapper,
                                           SysUserMapper sysUserMapper) {
        this.notificationOrchestrator = notificationOrchestrator;
        this.wecomGroupPushService = wecomGroupPushService;
        this.ticketMapper = ticketMapper;
        this.ticketFollowerMapper = ticketFollowerMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCreated(TicketCreatedEvent event) {
        TicketPO ticket = ticketMapper.selectById(event.getTicketId());
        if (ticket == null) {
            return;
        }

        String title = "新工单待处理 - " + safe(ticket.getTicketNo());
        String content = "工单编号：" + safe(ticket.getTicketNo()) +
                "\n标题：" + safe(ticket.getTitle()) +
                "\n优先级：" + safe(ticket.getPriority());

        if (ticket.getAssigneeId() != null) {
            notificationOrchestrator.dispatch(ticket.getAssigneeId(), ticket.getId(), null,
                    NotificationType.TICKET_CREATED, title, content);
        }
        wecomGroupPushService.pushByTicket(ticket.getId(), title, content);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketAssigned(TicketAssignedEvent event) {
        TicketPO ticket = ticketMapper.selectById(event.getTicketId());
        if (ticket == null || event.getAssigneeId() == null) {
            return;
        }

        String operatorName = resolveUserName(event.getOperatorId());
        String title = "您有新的工单分派 - " + safe(ticket.getTicketNo());
        String content = "工单编号：" + safe(ticket.getTicketNo()) +
                "\n标题：" + safe(ticket.getTitle()) +
                "\n分派人：" + safe(operatorName) +
                "\n优先级：" + safe(ticket.getPriority());

        notificationOrchestrator.dispatch(event.getAssigneeId(), ticket.getId(), null,
                NotificationType.ASSIGNED, title, content);
        wecomGroupPushService.pushByTicket(ticket.getId(), title, content);
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
        String title = "工单状态更新 - " + safe(ticket.getTicketNo());
        String content = "工单编号：" + safe(ticket.getTicketNo()) +
                "\n标题：" + safe(ticket.getTitle()) +
                "\n状态：" + oldStatus + " → " + newStatus +
                "\n操作人：" + safe(operatorName);

        Set<Long> receiverIds = new LinkedHashSet<>();
        if (ticket.getCreatorId() != null) {
            receiverIds.add(ticket.getCreatorId());
        }
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
        wecomGroupPushService.pushByTicket(ticket.getId(), title, content);
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

    private String resolveStatusLabel(String code) {
        if (code == null) {
            return "-";
        }
        TicketStatus status = TicketStatus.fromCode(code.toLowerCase());
        return status != null ? status.getLabel() : code;
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}
