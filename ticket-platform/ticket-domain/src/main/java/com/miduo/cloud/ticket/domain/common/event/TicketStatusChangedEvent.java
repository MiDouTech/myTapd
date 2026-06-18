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
    /**
     * 同次流转若伴随处理人变更，将原处理人并入群 @ 名单（分派群通知被合并时）。
     */
    private final Long previousAssigneeIdForGroupMention;

    public TicketStatusChangedEvent(Long ticketId, String oldStatus, String newStatus, Long operatorId) {
        this(ticketId, oldStatus, newStatus, operatorId, null);
    }

    public TicketStatusChangedEvent(Long ticketId, String oldStatus, String newStatus, Long operatorId,
                                    Long previousAssigneeIdForGroupMention) {
        super();
        this.ticketId = ticketId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.operatorId = operatorId;
        this.previousAssigneeIdForGroupMention = previousAssigneeIdForGroupMention;
    }
}
