package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * 工单创建后触发自动分派（在事务提交后处理，与 Webhook 等监听器一致）。
 */
@Getter
public class TicketAutoDispatchEvent extends DomainEvent {

    private final Long ticketId;

    public TicketAutoDispatchEvent(Long ticketId) {
        super();
        this.ticketId = ticketId;
    }
}
