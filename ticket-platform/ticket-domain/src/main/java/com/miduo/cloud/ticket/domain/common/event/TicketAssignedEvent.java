package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * 工单分派事件
 */
@Getter
public class TicketAssignedEvent extends DomainEvent {

    private final Long ticketId;
    private final Long assigneeId;
    private final Long previousAssigneeId;
    private final Long operatorId;
    private final String assignType;

    public TicketAssignedEvent(Long ticketId, Long assigneeId, Long previousAssigneeId,
                               Long operatorId, String assignType) {
        super();
        this.ticketId = ticketId;
        this.assigneeId = assigneeId;
        this.previousAssigneeId = previousAssigneeId;
        this.operatorId = operatorId;
        this.assignType = assignType;
    }
}
