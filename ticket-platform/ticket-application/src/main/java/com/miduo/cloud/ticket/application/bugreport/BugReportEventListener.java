package com.miduo.cloud.ticket.application.bugreport;

import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.domain.common.event.TicketCompletedEvent;
import org.springframework.util.StringUtils;
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
        // 「已关闭」通常为无效/非缺陷收尾，不强制生成简报草稿与关联；「已完成」等终态仍走原闭环
        if (StringUtils.hasText(event.getFinalStatus())
                && TicketStatus.CLOSED.getCode().equalsIgnoreCase(event.getFinalStatus().trim())) {
            return;
        }
        bugReportApplicationService.createDraftFromClosedTicket(event.getTicketId(), event.getOperatorId());
    }
}
