package com.miduo.cloud.ticket.application.webhook;

import com.miduo.cloud.ticket.common.enums.WebhookEventType;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketCreatedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import lombok.Data;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 工单Webhook事件监听器
 */
@Component
public class TicketWebhookEventListener {

    private final WebhookDispatchService webhookDispatchService;

    public TicketWebhookEventListener(WebhookDispatchService webhookDispatchService) {
        this.webhookDispatchService = webhookDispatchService;
    }

    @EventListener
    public void onTicketCreated(TicketCreatedEvent event) {
        TicketCreatedPayload payload = new TicketCreatedPayload();
        payload.setCategoryId(event.getCategoryId());
        payload.setPriority(event.getPriority());
        webhookDispatchService.dispatch(WebhookEventType.TICKET_CREATED, event.getTicketId(), payload);
    }

    @EventListener
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        TicketStatusChangedPayload payload = new TicketStatusChangedPayload();
        payload.setOldStatus(event.getOldStatus());
        payload.setNewStatus(event.getNewStatus());
        payload.setOperatorId(event.getOperatorId());
        webhookDispatchService.dispatch(WebhookEventType.TICKET_STATUS_CHANGED, event.getTicketId(), payload);
    }

    @EventListener
    public void onTicketAssigned(TicketAssignedEvent event) {
        TicketAssignedPayload payload = new TicketAssignedPayload();
        payload.setAssigneeId(event.getAssigneeId());
        payload.setPreviousAssigneeId(event.getPreviousAssigneeId());
        payload.setOperatorId(event.getOperatorId());
        payload.setAssignType(event.getAssignType());
        webhookDispatchService.dispatch(WebhookEventType.TICKET_ASSIGNED, event.getTicketId(), payload);
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
