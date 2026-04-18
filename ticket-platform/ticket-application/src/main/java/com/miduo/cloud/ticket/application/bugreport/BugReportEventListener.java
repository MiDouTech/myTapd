package com.miduo.cloud.ticket.application.bugreport;

import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.domain.common.event.TicketCompletedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
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

    /**
     * 缺陷工单进入「临时解决」后也自动建立（或复用）简报关联，
     * 让工单详情在该状态下可直接展示并跳转到关联 Bug 简报。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        if (!StringUtils.hasText(event.getNewStatus())) {
            return;
        }
        if (!TicketStatus.TEMP_RESOLVED.getCode().equalsIgnoreCase(event.getNewStatus().trim())) {
            return;
        }
        bugReportApplicationService.createDraftFromClosedTicket(event.getTicketId(), event.getOperatorId());
    }
}
