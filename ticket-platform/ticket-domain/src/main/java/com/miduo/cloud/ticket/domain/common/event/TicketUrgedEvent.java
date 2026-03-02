package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * 工单催办事件
 */
@Getter
public class TicketUrgedEvent extends DomainEvent {

    private final Long ticketId;
    private final Long urgerId;
    private final Long handlerId;

    public TicketUrgedEvent(Long ticketId, Long urgerId, Long handlerId) {
        super();
        this.ticketId = ticketId;
        this.urgerId = urgerId;
        this.handlerId = handlerId;
    }
}
