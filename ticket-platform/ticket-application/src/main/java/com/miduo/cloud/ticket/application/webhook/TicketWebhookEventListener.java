package com.miduo.cloud.ticket.application.webhook;

import com.miduo.cloud.ticket.common.enums.WebhookEventType;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 工单Webhook事件监听器
 */
@Component
public class TicketWebhookEventListener {

    private static final Logger log = LoggerFactory.getLogger(TicketWebhookEventListener.class);

    private final WebhookDispatchService webhookDispatchService;

    public TicketWebhookEventListener(WebhookDispatchService webhookDispatchService) {
        this.webhookDispatchService = webhookDispatchService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCreated(TicketCreatedEvent event) {
        try {
            log.info("接收工单创建事件并触发Webhook分发: eventId={}, ticketId={}, categoryId={}, priority={}",
                    event.getEventId(), event.getTicketId(), event.getCategoryId(), event.getPriority());
            TicketCreatedPayload payload = new TicketCreatedPayload();
            payload.setCategoryId(event.getCategoryId());
            payload.setPriority(event.getPriority());
            webhookDispatchService.dispatch(WebhookEventType.TICKET_CREATED, event.getTicketId(), payload);
        } catch (Exception ex) {
            log.error("处理工单创建Webhook事件失败: ticketId={}", event != null ? event.getTicketId() : null, ex);
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
            TicketAssignedPayload payload = new TicketAssignedPayload();
            payload.setAssigneeId(event.getAssigneeId());
            payload.setPreviousAssigneeId(event.getPreviousAssigneeId());
            payload.setOperatorId(event.getOperatorId());
            payload.setAssignType(event.getAssignType());
            webhookDispatchService.dispatch(WebhookEventType.TICKET_ASSIGNED, event.getTicketId(), payload);
        } catch (Exception ex) {
            log.error("处理工单分派Webhook事件失败: ticketId={}", event != null ? event.getTicketId() : null, ex);
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
        private Long previousAssigneeId;
        private Long operatorId;
        private String assignType;
    }
}
