package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * 工单创建且自动分派尝试完成后触发，用于 Webhook/通知等「创建」类对外推送。
 * <p>与 {@link TicketCreatedEvent} 中 {@code pendingAutoDispatch=true} 配对：
 * 避免异步监听与 {@code REQUIRES_NEW} 自动分派并发读库得到过时状态。</p>
 */
@Getter
public class TicketCreatedAfterAutoDispatchEvent extends DomainEvent {

    private final Long ticketId;
    private final Long categoryId;
    private final String priority;

    public TicketCreatedAfterAutoDispatchEvent(Long ticketId, Long categoryId, String priority) {
        super();
        this.ticketId = ticketId;
        this.categoryId = categoryId;
        this.priority = priority;
    }
}
