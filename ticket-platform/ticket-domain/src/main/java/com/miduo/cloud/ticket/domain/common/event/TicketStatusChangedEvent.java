package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * 工单状态变更事件
 */
@Getter
public class TicketStatusChangedEvent extends DomainEvent {

    private final Long ticketId;
    private final String oldStatus;
    private final String newStatus;
    private final Long operatorId;

    public TicketStatusChangedEvent(Long ticketId, String oldStatus, String newStatus, Long operatorId) {
        super();
        this.ticketId = ticketId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.operatorId = operatorId;
    }
}
