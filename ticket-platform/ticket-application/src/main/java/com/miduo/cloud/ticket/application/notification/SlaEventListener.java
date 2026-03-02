package com.miduo.cloud.ticket.application.notification;

import com.miduo.cloud.ticket.common.enums.NotificationType;
import com.miduo.cloud.ticket.domain.common.event.SlaBreachedEvent;
import com.miduo.cloud.ticket.domain.common.event.SlaWarningEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketUrgedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * SLA和催办事件监听器
 * 监听SLA预警/超时和催办事件，触发通知分发
 */
@Component
public class SlaEventListener {

    private static final Logger log = LoggerFactory.getLogger(SlaEventListener.class);

    private final NotificationOrchestrator orchestrator;

    public SlaEventListener(NotificationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Async
    @EventListener
    public void onSlaWarning(SlaWarningEvent event) {
        log.info("接收到SLA预警事件: ticketId={}, timerType={}, level={}",
                event.getTicketId(), event.getTimerType(), event.getSlaLevel());

        String title = String.format("SLA预警 - 工单 #%d", event.getTicketId());
        String content = String.format("工单 #%d 的%s时限已使用 %d/%d 分钟，预警等级：%s",
                event.getTicketId(),
                "RESPONSE".equals(event.getTimerType()) ? "响应" : "解决",
                event.getElapsedMinutes(),
                event.getThresholdMinutes(),
                event.getSlaLevel());

        // TODO: 查询工单处理人ID，向处理人及其上级发送通知
        // 此处预留，待工单服务完善后补充处理人查询逻辑
        log.info("SLA预警通知待分发（预留处理人查询）: ticketId={}", event.getTicketId());
    }

    @Async
    @EventListener
    public void onSlaBreached(SlaBreachedEvent event) {
        log.info("接收到SLA超时事件: ticketId={}, timerType={}",
                event.getTicketId(), event.getTimerType());

        String title = String.format("SLA超时 - 工单 #%d", event.getTicketId());
        String content = String.format("工单 #%d 的%s时限已超时，已用 %d 分钟，限时 %d 分钟",
                event.getTicketId(),
                "RESPONSE".equals(event.getTimerType()) ? "响应" : "解决",
                event.getElapsedMinutes(),
                event.getThresholdMinutes());

        // TODO: 查询工单处理人ID及管理员，向处理人、上级和管理员发送通知
        log.info("SLA超时通知待分发（预留处理人查询）: ticketId={}", event.getTicketId());
    }

    @Async
    @EventListener
    public void onTicketUrged(TicketUrgedEvent event) {
        log.info("接收到催办事件: ticketId={}, urgerId={}, handlerId={}",
                event.getTicketId(), event.getUrgerId(), event.getHandlerId());

        if (event.getHandlerId() == null) {
            log.warn("催办事件处理人ID为空，跳过通知: ticketId={}", event.getTicketId());
            return;
        }

        String title = String.format("工单催办 - 工单 #%d", event.getTicketId());
        String content = String.format("工单 #%d 被催办，请尽快处理", event.getTicketId());

        orchestrator.dispatch(event.getHandlerId(), event.getTicketId(), null,
                NotificationType.URGE, title, content);
    }
}
