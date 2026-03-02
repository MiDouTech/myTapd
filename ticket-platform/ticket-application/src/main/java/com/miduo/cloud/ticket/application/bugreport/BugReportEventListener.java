package com.miduo.cloud.ticket.application.bugreport;

import com.miduo.cloud.ticket.domain.common.event.TicketCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Bug简报事件监听器
 */
@Component
public class BugReportEventListener {

    private final BugReportApplicationService bugReportApplicationService;

    public BugReportEventListener(BugReportApplicationService bugReportApplicationService) {
        this.bugReportApplicationService = bugReportApplicationService;
    }

    @Async
    @EventListener
    public void onTicketCompleted(TicketCompletedEvent event) {
        bugReportApplicationService.createDraftFromClosedTicket(event.getTicketId(), event.getOperatorId());
    }
}
