package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 工单催办事件
 */
@Getter
public class TicketUrgedEvent extends DomainEvent {

    private final Long ticketId;
    private final Long urgerId;
    /**
     * 接收催办通知的用户 ID 列表（已去重、非空）
     */
    private final List<Long> notifyUserIds;

    public TicketUrgedEvent(Long ticketId, Long urgerId, List<Long> notifyUserIds) {
        super();
        this.ticketId = ticketId;
        this.urgerId = urgerId;
        this.notifyUserIds = notifyUserIds != null ? notifyUserIds : Collections.emptyList();
    }
}
