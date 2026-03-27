package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.dailyreport.DailyReportApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 日报自动推送定时任务
 * 支持多个推送时间点，通过轮询检测配置中的多个 cron 表达式来动态触发推送
 */
@Component
public class DailyReportJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportJobHandler.class);

    private final DailyReportApplicationService dailyReportService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile List<String> activeCronList = Collections.emptyList();
    private volatile Map<String, Date> nextExecuteTimeMap = new HashMap<>();

    public DailyReportJobHandler(DailyReportApplicationService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    /**
     * 每30秒轮询一次，检查是否有 cron 触发时间已到达，若到达则执行日报推送。
     * 支持多个 cron 表达式，每个时间点独立触发。
     */
    @Scheduled(fixedDelay = 30000)
    public void execute() {
        if (!dailyReportService.isEnabled()) {
            return;
        }

        List<String> cronList = dailyReportService.getPushCronList();
        if (cronList == null || cronList.isEmpty()) {
            activeCronList = Collections.emptyList();
            nextExecuteTimeMap = new HashMap<>();
            return;
        }

        Date now = new Date();

        if (!cronList.equals(activeCronList)) {
            activeCronList = new ArrayList<>(cronList);
            Map<String, Date> newMap = new HashMap<>();
            for (String cron : cronList) {
                try {
                    Date next = nextExecutionTime(cron, now);
                    newMap.put(cron, next);
                    log.info("日报推送：加载cron={}, nextExecuteTime={}", cron, next);
                } catch (IllegalArgumentException ex) {
                    log.error("日报推送：cron表达式非法，cron={}", cron, ex);
                }
            }
            nextExecuteTimeMap = newMap;
            return;
        }

        for (String cron : activeCronList) {
            Date nextTime = nextExecuteTimeMap.get(cron);
            if (nextTime == null) {
                continue;
            }
            if (!now.before(nextTime)) {
                triggerPush(cron, now);
            }
        }
    }

    private void triggerPush(String cron, Date now) {
        if (!running.compareAndSet(false, true)) {
            log.warn("日报推送：推送任务仍在执行，跳过本轮触发, cron={}", cron);
            return;
        }
        log.info("日报推送：开始执行，触发cron={}", cron);
        try {
            dailyReportService.pushDailyReport();
            log.info("日报推送：执行完成，触发cron={}", cron);
        } catch (Exception ex) {
            log.error("日报推送：执行异常，触发cron={}", cron, ex);
        } finally {
            running.set(false);
            try {
                Date next = nextExecutionTime(cron, new Date());
                nextExecuteTimeMap.put(cron, next);
            } catch (Exception ex) {
                log.error("日报推送：计算下一次触发时间失败，cron={}", cron, ex);
                nextExecuteTimeMap.remove(cron);
            }
        }
    }

    private Date nextExecutionTime(String cron, Date baseTime) {
        SimpleTriggerContext triggerContext = new SimpleTriggerContext();
        triggerContext.update(baseTime, baseTime, baseTime);
        return new CronTrigger(cron).nextExecutionTime(triggerContext);
    }
}
