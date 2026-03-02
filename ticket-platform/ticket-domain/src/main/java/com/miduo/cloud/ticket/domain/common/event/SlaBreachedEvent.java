package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * SLA超时事件
 */
@Getter
public class SlaBreachedEvent extends DomainEvent {

    private final Long ticketId;
    private final Long slaTimerId;
    private final String timerType;
    private final int elapsedMinutes;
    private final int thresholdMinutes;

    public SlaBreachedEvent(Long ticketId, Long slaTimerId, String timerType,
                            int elapsedMinutes, int thresholdMinutes) {
        super();
        this.ticketId = ticketId;
        this.slaTimerId = slaTimerId;
        this.timerType = timerType;
        this.elapsedMinutes = elapsedMinutes;
        this.thresholdMinutes = thresholdMinutes;
    }
}
