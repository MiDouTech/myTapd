package com.miduo.cloud.ticket.application.notification;

import com.miduo.cloud.ticket.common.enums.NotificationType;
import com.miduo.cloud.ticket.domain.common.event.SlaBreachedEvent;
import com.miduo.cloud.ticket.domain.common.event.SlaWarningEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketUrgedEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.HandlerGroupMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.HandlerGroupPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SLA和催办事件监听器
 * 监听SLA预警/超时和催办事件，触发通知分发
 */
@Component
public class SlaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SlaEventListener.class);

    private final NotificationOrchestrator orchestrator;
    private final WecomGroupPushService wecomGroupPushService;
    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final HandlerGroupMapper handlerGroupMapper;

    public SlaEventListener(NotificationOrchestrator orchestrator,
                            WecomGroupPushService wecomGroupPushService,
                            TicketMapper ticketMapper,
                            TicketCategoryMapper ticketCategoryMapper,
                            HandlerGroupMapper handlerGroupMapper) {
        this.orchestrator = orchestrator;
        this.wecomGroupPushService = wecomGroupPushService;
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.handlerGroupMapper = handlerGroupMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSlaWarning(SlaWarningEvent event) {
        log.info("接收到SLA预警事件: ticketId={}, timerType={}, level={}",
                event.getTicketId(), event.getTimerType(), event.getSlaLevel());

        TicketPO ticket = getTicket(event.getTicketId());
        if (ticket == null) {
            return;
        }

        String ticketRef = ticket.getTicketNo() != null ? ticket.getTicketNo()
                : "#" + event.getTicketId();
        String title = String.format("SLA预警 - 工单 %s", ticketRef);
        String content = String.format("工单 %s 的%s时限已使用 %d/%d 分钟，预警等级：%s",
                ticketRef,
                "RESPONSE".equals(event.getTimerType()) ? "响应" : "解决",
                event.getElapsedMinutes(),
                event.getThresholdMinutes(),
                event.getSlaLevel());

        if (ticket.getAssigneeId() != null) {
            orchestrator.dispatch(ticket.getAssigneeId(), ticket.getId(), null,
                    NotificationType.SLA_WARNING, title, content);
        }
        LinkedHashSet<Long> mentionUserIds = new LinkedHashSet<>();
        if (ticket.getCreatorId() != null) {
            mentionUserIds.add(ticket.getCreatorId());
        }
        if (ticket.getAssigneeId() != null) {
            mentionUserIds.add(ticket.getAssigneeId());
        }
        wecomGroupPushService.pushByTicketWithUserMentions(event.getTicketId(), title, content, mentionUserIds);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSlaBreached(SlaBreachedEvent event) {
        log.info("接收到SLA超时事件: ticketId={}, timerType={}",
                event.getTicketId(), event.getTimerType());

        TicketPO ticket = getTicket(event.getTicketId());
        if (ticket == null) {
            return;
        }

        String ticketRef = ticket.getTicketNo() != null ? ticket.getTicketNo()
                : "#" + event.getTicketId();
        String title = String.format("SLA超时 - 工单 %s", ticketRef);
        String content = String.format("工单 %s 的%s时限已超时，已用 %d 分钟，限时 %d 分钟",
                ticketRef,
                "RESPONSE".equals(event.getTimerType()) ? "响应" : "解决",
                event.getElapsedMinutes(),
                event.getThresholdMinutes());

        Set<Long> receivers = new LinkedHashSet<>();
        if (ticket.getAssigneeId() != null) {
            receivers.add(ticket.getAssigneeId());
        }
        Long groupLeaderId = resolveGroupLeaderId(ticket.getCategoryId());
        if (groupLeaderId != null) {
            receivers.add(groupLeaderId);
        }
        if (!receivers.isEmpty()) {
            orchestrator.dispatchToUsers(new ArrayList<>(receivers), ticket.getId(), null,
                    NotificationType.SLA_BREACHED, title, content);
        }
        LinkedHashSet<Long> mentionUserIds = new LinkedHashSet<>();
        if (ticket.getCreatorId() != null) {
            mentionUserIds.add(ticket.getCreatorId());
        }
        if (ticket.getAssigneeId() != null) {
            mentionUserIds.add(ticket.getAssigneeId());
        }
        if (groupLeaderId != null) {
            mentionUserIds.add(groupLeaderId);
        }
        wecomGroupPushService.pushByTicketWithUserMentions(event.getTicketId(), title, content, mentionUserIds);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketUrged(TicketUrgedEvent event) {
        log.info("接收到催办事件: ticketId={}, urgerId={}, notifyUserIds={}",
                event.getTicketId(), event.getUrgerId(), event.getNotifyUserIds());

        if (event.getNotifyUserIds() == null || event.getNotifyUserIds().isEmpty()) {
            log.warn("催办事件通知对象为空，跳过通知: ticketId={}", event.getTicketId());
            return;
        }

        TicketPO ticket = getTicket(event.getTicketId());
        String ticketRef = (ticket != null && ticket.getTicketNo() != null)
                ? ticket.getTicketNo() : "#" + event.getTicketId();

        String title = String.format("工单催办 - 工单 %s", ticketRef);
        String content = String.format("工单 %s 被催办，请尽快处理", ticketRef);

        orchestrator.dispatchToUsers(new ArrayList<>(event.getNotifyUserIds()), event.getTicketId(), null,
                NotificationType.URGE, title, content);
        LinkedHashSet<Long> mentionUserIds = new LinkedHashSet<>(event.getNotifyUserIds());
        if (ticket != null && ticket.getCreatorId() != null) {
            mentionUserIds.add(ticket.getCreatorId());
        }
        if (event.getUrgerId() != null) {
            mentionUserIds.add(event.getUrgerId());
        }
        wecomGroupPushService.pushByTicketWithUserMentions(event.getTicketId(), title, content, mentionUserIds);
    }

    private TicketPO getTicket(Long ticketId) {
        if (ticketId == null) {
            return null;
        }
        return ticketMapper.selectById(ticketId);
    }

    private Long resolveGroupLeaderId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        TicketCategoryPO category = ticketCategoryMapper.selectById(categoryId);
        if (category == null || category.getDefaultGroupId() == null) {
            return null;
        }
        HandlerGroupPO group = handlerGroupMapper.selectById(category.getDefaultGroupId());
        return group != null ? group.getLeaderId() : null;
    }
}
