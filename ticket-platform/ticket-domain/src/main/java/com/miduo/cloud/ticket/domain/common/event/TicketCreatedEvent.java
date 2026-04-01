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
    /**
     * 为 true 时表示创建后还将执行自动分派；Webhook/站内「创建」类推送应延后到
     * {@link TicketCreatedAfterAutoDispatchEvent}，避免与 {@code REQUIRES_NEW} 分派事务竞态导致快照仍为待分派。
     */
    private final boolean pendingAutoDispatch;

    public TicketCreatedEvent(Long ticketId, Long categoryId, String priority, boolean pendingAutoDispatch) {
        super();
        this.ticketId = ticketId;
        this.categoryId = categoryId;
        this.priority = priority;
        this.pendingAutoDispatch = pendingAutoDispatch;
    }
}
