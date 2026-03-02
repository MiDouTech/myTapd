package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

/**
 * SLA预警事件
 */
@Getter
public class SlaWarningEvent extends DomainEvent {

    private final Long ticketId;
    private final Long slaTimerId;
    private final String timerType;
    private final int elapsedMinutes;
    private final int thresholdMinutes;
    private final String slaLevel;

    public SlaWarningEvent(Long ticketId, Long slaTimerId, String timerType,
                           int elapsedMinutes, int thresholdMinutes, String slaLevel) {
        super();
        this.ticketId = ticketId;
        this.slaTimerId = slaTimerId;
        this.timerType = timerType;
        this.elapsedMinutes = elapsedMinutes;
        this.thresholdMinutes = thresholdMinutes;
        this.slaLevel = slaLevel;
    }
}
