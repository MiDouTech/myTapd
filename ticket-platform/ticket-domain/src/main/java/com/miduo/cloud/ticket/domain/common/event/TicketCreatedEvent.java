package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * 工单创建事件
 */
@Getter
public class TicketCreatedEvent extends DomainEvent {

    private final Long ticketId;
    private final Long categoryId;
    private final String priority;

    public TicketCreatedEvent(Long ticketId, Long categoryId, String priority) {
        super();
        this.ticketId = ticketId;
        this.categoryId = categoryId;
        this.priority = priority;
    }
}
