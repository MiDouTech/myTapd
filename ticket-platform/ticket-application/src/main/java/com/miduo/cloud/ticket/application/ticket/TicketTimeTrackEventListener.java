package com.miduo.cloud.ticket.application.ticket;

import com.miduo.cloud.ticket.domain.common.event.TicketTimeTrackRecordedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 工单时间追踪事件监听器
 */
@Component
public class TicketTimeTrackEventListener {

    private final TicketNodeDurationApplicationService nodeDurationApplicationService;

    public TicketTimeTrackEventListener(TicketNodeDurationApplicationService nodeDurationApplicationService) {
        this.nodeDurationApplicationService = nodeDurationApplicationService;
    }

    @Async
    @EventListener
    public void onTimeTrackRecorded(TicketTimeTrackRecordedEvent event) {
        nodeDurationApplicationService.handleTrackEvent(event);
    }
}
