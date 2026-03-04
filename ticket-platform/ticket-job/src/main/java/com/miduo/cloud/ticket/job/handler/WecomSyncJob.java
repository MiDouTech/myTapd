package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.wecom.WecomSyncApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 企微通讯录同步定时任务
 * 动态读取配置并触发全量同步
 */
@Component
public class WecomSyncJob {

    private static final Logger log = LoggerFactory.getLogger(WecomSyncJob.class);

    private final WecomSyncApplicationService wecomSyncApplicationService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String activeCron;
    private volatile Date nextExecuteTime;

    public WecomSyncJob(WecomSyncApplicationService wecomSyncApplicationService) {
        this.wecomSyncApplicationService = wecomSyncApplicationService;
    }

    /**
     * 轮询配置并按cron触发同步
     */
    @Scheduled(fixedDelayString = "${wecom.sync.scheduler-check-delay-ms:30000}")
    public void syncWecomContacts() {
        WecomSyncApplicationService.ScheduleConfig scheduleConfig = wecomSyncApplicationService.getScheduleConfig();
        if (scheduleConfig == null
                || !Boolean.TRUE.equals(scheduleConfig.getScheduleEnabled())
                || isBlank(scheduleConfig.getScheduleCron())) {
            activeCron = null;
            nextExecuteTime = null;
            return;
        }

        String cron = scheduleConfig.getScheduleCron().trim();
        Date now = new Date();
        try {
            if (!cron.equals(activeCron) || nextExecuteTime == null) {
                activeCron = cron;
                nextExecuteTime = nextExecutionTime(cron, now);
                log.info("定时任务：已加载同步cron={}, nextExecuteTime={}", cron, nextExecuteTime);
                return;
            }
        } catch (IllegalArgumentException ex) {
            log.error("定时任务：cron表达式非法，cron={}", cron, ex);
            activeCron = null;
            nextExecuteTime = null;
            return;
        }

        if (nextExecuteTime == null || now.before(nextExecuteTime)) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.warn("定时任务：同步任务仍在执行，跳过本轮触发");
            return;
        }

        log.info("定时任务：开始企微通讯录全量同步...");
        try {
            wecomSyncApplicationService.scheduleSync();
            log.info("定时任务：企微通讯录全量同步完成");
        } catch (Exception e) {
            log.error("定时任务：企微通讯录全量同步失败", e);
        } finally {
            running.set(false);
            try {
                nextExecuteTime = nextExecutionTime(activeCron, new Date());
            } catch (Exception ex) {
                log.error("定时任务：计算下一次触发时间失败，cron={}", activeCron, ex);
                activeCron = null;
                nextExecuteTime = null;
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Date nextExecutionTime(String cron, Date baseTime) {
        SimpleTriggerContext triggerContext = new SimpleTriggerContext();
        triggerContext.update(baseTime, baseTime, baseTime);
        return new CronTrigger(cron).nextExecutionTime(triggerContext);
    }
}
