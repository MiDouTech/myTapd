package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

import java.util.Date;

/**
 * 工单时间追踪记录事件
 * 用于驱动节点耗时统计异步更新。
 */
@Getter
public class TicketTimeTrackRecordedEvent extends DomainEvent {

    private final Long ticketId;
    private final Long userId;
    private final String userRole;
    private final String action;
    private final String fromStatus;
    private final String toStatus;
    private final Long fromUserId;
    private final Long toUserId;
    private final Date timestamp;

    public TicketTimeTrackRecordedEvent(Long ticketId, Long userId, String userRole,
                                        String action, String fromStatus, String toStatus,
                                        Long fromUserId, Long toUserId, Date timestamp) {
        super();
        this.ticketId = ticketId;
        this.userId = userId;
        this.userRole = userRole;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.timestamp = timestamp;
    }
}
