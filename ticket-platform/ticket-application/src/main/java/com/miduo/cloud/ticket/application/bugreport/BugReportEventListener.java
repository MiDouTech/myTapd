package com.miduo.cloud.ticket.application.bugreport;

import com.miduo.cloud.ticket.domain.common.event.TicketCompletedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCompleted(TicketCompletedEvent event) {
        bugReportApplicationService.createDraftFromClosedTicket(event.getTicketId(), event.getOperatorId());
    }
}
