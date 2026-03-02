package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

import java.util.Date;

/**
 * 工单完成/关闭事件
 */
@Getter
public class TicketCompletedEvent extends DomainEvent {

    private final Long ticketId;
    private final String finalStatus;
    private final Long operatorId;
    private final Date completedAt;

    public TicketCompletedEvent(Long ticketId, String finalStatus, Long operatorId, Date completedAt) {
        super();
        this.ticketId = ticketId;
        this.finalStatus = finalStatus;
        this.operatorId = operatorId;
        this.completedAt = completedAt;
    }
}
