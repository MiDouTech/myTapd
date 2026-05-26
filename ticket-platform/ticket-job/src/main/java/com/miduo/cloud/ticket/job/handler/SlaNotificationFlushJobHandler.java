package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.notification.SlaNotificationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SLA 延迟通知发送任务
 * 每分钟扫描静默队列，在计划时间（默认次日 09:00）发送
 */
@Component
public class SlaNotificationFlushJobHandler {

    private static final Logger log = LoggerFactory.getLogger(SlaNotificationFlushJobHandler.class);

    private final SlaNotificationDispatchService slaNotificationDispatchService;

    public SlaNotificationFlushJobHandler(SlaNotificationDispatchService slaNotificationDispatchService) {
        this.slaNotificationDispatchService = slaNotificationDispatchService;
    }

    @Scheduled(fixedRate = 60000)
    public void execute() {
        try {
            int sent = slaNotificationDispatchService.flushDuePendingNotifications();
            if (sent > 0) {
                log.info("SLA延迟通知发送完成: count={}", sent);
            }
        } catch (Exception e) {
            log.error("SLA延迟通知发送任务执行异常", e);
        }
    }
}
