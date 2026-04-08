package com.miduo.cloud.ticket.application.webhook;

import com.miduo.cloud.ticket.application.ticket.TicketAssigneeSyncService;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.enums.WebhookEventType;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCommentMentionEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedAfterAutoDispatchEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 工单Webhook事件监听器
 */
@Component
public class TicketWebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(TicketWebhookEventListener.class);

    private final WebhookDispatchService webhookDispatchService;
    private final TicketAssigneeSyncService ticketAssigneeSyncService;

    public TicketWebhookEventListener(WebhookDispatchService webhookDispatchService,
                                      TicketAssigneeSyncService ticketAssigneeSyncService) {
        this.webhookDispatchService = webhookDispatchService;
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
        dispatchTicketCreatedWebhook(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCreatedAfterAutoDispatch(TicketCreatedAfterAutoDispatchEvent event) {
        dispatchTicketCreatedWebhook(event);
    }

    private void dispatchTicketCreatedWebhook(Object event) {
        if (event == null) {
            return;
        }
        Long ticketId;
        Long categoryId;
        String priority;
        String eventId;
        if (event instanceof TicketCreatedEvent) {
            TicketCreatedEvent e = (TicketCreatedEvent) event;
            ticketId = e.getTicketId();
            categoryId = e.getCategoryId();
            priority = e.getPriority();
            eventId = e.getEventId();
        } else if (event instanceof TicketCreatedAfterAutoDispatchEvent) {
            TicketCreatedAfterAutoDispatchEvent e = (TicketCreatedAfterAutoDispatchEvent) event;
            ticketId = e.getTicketId();
            categoryId = e.getCategoryId();
            priority = e.getPriority();
            eventId = e.getEventId();
        } else {
            return;
        }
        try {
            log.info("接收工单创建事件并触发Webhook分发: eventId={}, ticketId={}, categoryId={}, priority={}",
                    eventId, ticketId, categoryId, priority);
            TicketCreatedPayload payload = new TicketCreatedPayload();
            payload.setCategoryId(categoryId);
            payload.setPriority(priority);
            webhookDispatchService.dispatch(WebhookEventType.TICKET_CREATED, ticketId, payload);
        } catch (Exception ex) {
            log.error("处理工单创建Webhook事件失败: ticketId={}", ticketId, ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        try {
            log.info("接收工单状态变更事件并触发Webhook分发: eventId={}, ticketId={}, oldStatus={}, newStatus={}, operatorId={}",
                    event.getEventId(), event.getTicketId(), event.getOldStatus(), event.getNewStatus(), event.getOperatorId());
            TicketStatusChangedPayload payload = new TicketStatusChangedPayload();
            payload.setOldStatus(event.getOldStatus());
            payload.setNewStatus(event.getNewStatus());
            payload.setOperatorId(event.getOperatorId());
            webhookDispatchService.dispatch(WebhookEventType.TICKET_STATUS_CHANGED, event.getTicketId(), payload);

            TicketStatus newStatus = TicketStatus.fromCode(event.getNewStatus());
            if (newStatus == TicketStatus.COMPLETED) {
                webhookDispatchService.dispatch(WebhookEventType.TICKET_COMPLETED, event.getTicketId(), payload);
            } else if (newStatus == TicketStatus.CLOSED) {
                webhookDispatchService.dispatch(WebhookEventType.TICKET_CLOSED, event.getTicketId(), payload);
            }
        } catch (Exception ex) {
            log.error("处理工单状态变更Webhook事件失败: ticketId={}", event != null ? event.getTicketId() : null, ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketAssigned(TicketAssignedEvent event) {
        try {
            log.info("接收工单分派事件并触发Webhook分发: eventId={}, ticketId={}, assigneeId={}, previousAssigneeId={}, operatorId={}, assignType={}",
                    event.getEventId(), event.getTicketId(), event.getAssigneeId(),
                    event.getPreviousAssigneeId(), event.getOperatorId(), event.getAssignType());
            // 读取工单当前有效处理人集合（主处理人 + 协同处理人），避免追加处理人场景只展示旧主处理人。
            java.util.List<Long> currentAssigneeIds = ticketAssigneeSyncService.listActiveUserIds(event.getTicketId());
            TicketAssignedPayload payload = new TicketAssignedPayload();
            payload.setAssigneeId(event.getAssigneeId());
            payload.setAssigneeIds(currentAssigneeIds);
            payload.setPreviousAssigneeId(event.getPreviousAssigneeId());
            payload.setOperatorId(event.getOperatorId());
            payload.setAssignType(event.getAssignType());
            webhookDispatchService.dispatch(WebhookEventType.TICKET_ASSIGNED, event.getTicketId(), payload);
        } catch (Exception ex) {
            log.error("处理工单分派Webhook事件失败: ticketId={}", event != null ? event.getTicketId() : null, ex);
        }
    }

    /**
     * 评论 @ 的 Webhook 必须在事务提交后分发；不可与本方法再叠加
     * {@code org.springframework.scheduling.annotation.Async}，
     * 否则异步代理可能在提交前介入，导致 {@link TransactionPhase#AFTER_COMMIT} 不可靠（与站内通知监听器同类处理一致）。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCommentMention(TicketCommentMentionEvent event) {
        if (event == null || event.getTicketId() == null
                || event.getMentionedUserIds() == null || event.getMentionedUserIds().isEmpty()) {
            return;
        }
        try {
            log.info("接收评论@事件并触发Webhook分发: ticketId={}, mentionedCount={}, authorId={}",
                    event.getTicketId(), event.getMentionedUserIds().size(), event.getCommentAuthorUserId());
            CommentMentionWebhookPayload payload = new CommentMentionWebhookPayload();
            payload.setMentionedUserIds(new ArrayList<>(event.getMentionedUserIds()));
            payload.setCommentAuthorUserId(event.getCommentAuthorUserId());
            payload.setCommentPlainSummary(event.getCommentPlainSummary());
            webhookDispatchService.dispatch(WebhookEventType.TICKET_COMMENT_MENTION, event.getTicketId(), payload);
        } catch (Exception ex) {
            log.error("处理评论@Webhook事件失败: ticketId={}", event != null ? event.getTicketId() : null, ex);
        }
    }

    @Data
    private static class TicketCreatedPayload {
        private Long categoryId;
        private String priority;
    }

    @Data
    private static class TicketStatusChangedPayload {
        private String oldStatus;
        private String newStatus;
        private Long operatorId;
    }

    @Data
    private static class TicketAssignedPayload {
        private Long assigneeId;
        private java.util.List<Long> assigneeIds;
        private Long previousAssigneeId;
        private Long operatorId;
        private String assignType;
    }

    @Data
    private static class CommentMentionWebhookPayload {
        private List<Long> mentionedUserIds;
        private Long commentAuthorUserId;
        private String commentPlainSummary;
    }
}
