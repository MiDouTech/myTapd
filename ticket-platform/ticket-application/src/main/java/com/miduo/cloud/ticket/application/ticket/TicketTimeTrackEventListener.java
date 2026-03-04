package com.miduo.cloud.ticket.application.ticket;

import com.miduo.cloud.ticket.domain.common.event.TicketTimeTrackRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 工单时间追踪事件监听器
 */
@Component
public class TicketTimeTrackEventListener {

    private static final Logger log = LoggerFactory.getLogger(TicketTimeTrackEventListener.class);

    private final TicketNodeDurationApplicationService nodeDurationApplicationService;

    public TicketTimeTrackEventListener(TicketNodeDurationApplicationService nodeDurationApplicationService) {
        this.nodeDurationApplicationService = nodeDurationApplicationService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTimeTrackRecorded(TicketTimeTrackRecordedEvent event) {
        try {
            nodeDurationApplicationService.handleTrackEvent(event);
        } catch (Exception ex) {
            Long ticketId = event != null ? event.getTicketId() : null;
            // 节点耗时统计为衍生数据，异常时降级处理，避免影响主流程
            log.error("处理工单时间追踪事件失败，已降级跳过: ticketId={}", ticketId, ex);
        }
    }
}
