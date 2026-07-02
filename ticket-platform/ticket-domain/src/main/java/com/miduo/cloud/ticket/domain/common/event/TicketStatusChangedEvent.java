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
    /**
     * 创建后系统自动分派触发的首次出池流转：通知由 {@link TicketCreatedAfterAutoDispatchEvent} 统一发送，
     * 避免与「工单状态变更」重复推送同一条群消息。
     */
    private final boolean suppressNotification;

    public TicketStatusChangedEvent(Long ticketId, String oldStatus, String newStatus, Long operatorId) {
        this(ticketId, oldStatus, newStatus, operatorId, null, false);
    }

    public TicketStatusChangedEvent(Long ticketId, String oldStatus, String newStatus, Long operatorId,
                                    Long previousAssigneeIdForGroupMention) {
        this(ticketId, oldStatus, newStatus, operatorId, previousAssigneeIdForGroupMention, false);
    }

    public TicketStatusChangedEvent(Long ticketId, String oldStatus, String newStatus, Long operatorId,
                                    Long previousAssigneeIdForGroupMention, boolean suppressNotification) {
        super();
        this.ticketId = ticketId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.operatorId = operatorId;
        this.previousAssigneeIdForGroupMention = previousAssigneeIdForGroupMention;
        this.suppressNotification = suppressNotification;
    }
}
