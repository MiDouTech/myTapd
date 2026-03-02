package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.sla.SlaTimerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SLA定时检查任务
 * 每分钟扫描运行中的SLA计时器，触发预警和超时事件
 */
@Component
public class SlaCheckJobHandler {

    private static final Logger log = LoggerFactory.getLogger(SlaCheckJobHandler.class);

    private final SlaTimerService slaTimerService;

    public SlaCheckJobHandler(SlaTimerService slaTimerService) {
        this.slaTimerService = slaTimerService;
    }

    /**
     * 每分钟执行一次SLA检查
     */
    @Scheduled(fixedRate = 60000)
    public void execute() {
        log.debug("开始执行SLA定时检查...");
        try {
            slaTimerService.checkRunningTimers();
        } catch (Exception e) {
            log.error("SLA定时检查执行异常", e);
        }
        log.debug("SLA定时检查执行完成");
    }
}
