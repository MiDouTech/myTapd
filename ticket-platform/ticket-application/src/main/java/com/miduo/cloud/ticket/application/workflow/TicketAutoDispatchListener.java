package com.miduo.cloud.ticket.application.workflow;

import com.miduo.cloud.ticket.domain.common.event.TicketAutoDispatchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 在工单创建事务提交后再执行自动分派。
 * <p>与 {@link com.miduo.cloud.ticket.application.webhook.TicketWebhookEventListener} 相同：
 * {@code AFTER_COMMIT} + {@code fallbackExecution=true}，避免仅依赖
 * {@code TransactionSynchronization#afterCommit} 在异步入口（如企微 {@code @Async}）下未注册导致分派不执行。</p>
 */
@Component
public class TicketAutoDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(TicketAutoDispatchListener.class);

    private final DispatchAppService dispatchAppService;

    public TicketAutoDispatchListener(DispatchAppService dispatchAppService) {
        this.dispatchAppService = dispatchAppService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCreatedNeedAutoDispatch(TicketAutoDispatchEvent event) {
        if (event == null || event.getTicketId() == null) {
            return;
        }
        Long ticketId = event.getTicketId();
        try {
            log.info("[自动分派监听] 工单创建事务已提交，开始自动分派: ticketId={}", ticketId);
            dispatchAppService.autoDispatch(ticketId);
            log.info("[自动分派监听] 自动分派完成: ticketId={}", ticketId);
        } catch (Exception e) {
            log.error("[自动分派监听] 自动分派失败，不影响工单创建: ticketId={}, errorClass={}, error={}",
                    ticketId, e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
