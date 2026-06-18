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
    /**
     * 与状态变更等同次业务操作时置 true：群/Webhook 仅由状态变更通知覆盖，避免重复推送。
     */
    private final boolean suppressGroupNotification;

    public TicketAssignedEvent(Long ticketId, Long assigneeId, Long previousAssigneeId,
                               Long operatorId, String assignType) {
        this(ticketId, assigneeId, previousAssigneeId, operatorId, assignType, false);
    }

    public TicketAssignedEvent(Long ticketId, Long assigneeId, Long previousAssigneeId,
                               Long operatorId, String assignType, boolean suppressGroupNotification) {
        super();
        this.ticketId = ticketId;
        this.assigneeId = assigneeId;
        this.previousAssigneeId = previousAssigneeId;
        this.operatorId = operatorId;
        this.assignType = assignType;
        this.suppressGroupNotification = suppressGroupNotification;
    }
}
