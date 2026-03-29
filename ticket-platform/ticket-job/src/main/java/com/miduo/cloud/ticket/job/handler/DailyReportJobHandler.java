package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.dailyreport.DailyReportApplicationService;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 日报自动推送定时任务
 * 支持多个推送时间点，通过轮询检测配置中的多个 cron 表达式来动态触发推送。
 * 若同一轮存在多个 cron 同时命中，仅推送一次，避免重复消息。
 */
@Component
public class DailyReportJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportJobHandler.class);

    private final DailyReportApplicationService dailyReportService;
    private final StringRedisTemplate stringRedisTemplate;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile List<String> activeCronList = Collections.emptyList();
    private volatile Map<String, Date> nextExecuteTimeMap = new HashMap<>();
    private volatile String activeTimezone;

    public DailyReportJobHandler(DailyReportApplicationService dailyReportService,
                                 StringRedisTemplate stringRedisTemplate) {
        this.dailyReportService = dailyReportService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 每30秒轮询一次，检查是否有 cron 触发时间已到达，若到达则执行日报推送。
     * 支持多个 cron 表达式，每个时间点独立触发。
     */
    @Scheduled(fixedDelay = 30000)
    public void execute() {
        if (!dailyReportService.isEnabled()) {
            activeCronList = Collections.emptyList();
            nextExecuteTimeMap = new HashMap<>();
            activeTimezone = null;
            return;
        }

        List<String> cronList = dailyReportService.getPushCronList();
        String timezone = dailyReportService.getScheduleTimezone();
        if (cronList == null || cronList.isEmpty()) {
            activeCronList = Collections.emptyList();
            nextExecuteTimeMap = new HashMap<>();
            activeTimezone = timezone;
            return;
        }

        Date now = new Date();

        if (!cronList.equals(activeCronList) || !Objects.equals(timezone, activeTimezone)) {
            activeCronList = new ArrayList<>(cronList);
            Map<String, Date> newMap = new HashMap<>();
            for (String cron : cronList) {
                try {
                    Date next = nextExecutionTime(cron, now, timezone);
                    newMap.put(cron, next);
                    log.info("日报推送：加载cron={}, timezone={}, nextExecuteTime={}", cron, timezone, next);
                } catch (IllegalArgumentException ex) {
                    log.error("日报推送：cron表达式非法，cron={}", cron, ex);
                }
            }
            nextExecuteTimeMap = newMap;
            activeTimezone = timezone;
            return;
        }

        List<String> dueCrons = new ArrayList<>();
        for (String cron : activeCronList) {
            Date nextTime = nextExecuteTimeMap.get(cron);
            if (nextTime == null) {
                continue;
            }
            if (!now.before(nextTime)) {
                dueCrons.add(cron);
            }
        }
        if (dueCrons.isEmpty()) {
            return;
        }

        triggerPush(dueCrons, now, timezone);
    }

    private void triggerPush(List<String> dueCrons, Date now, String timezone) {
        if (!running.compareAndSet(false, true)) {
            log.warn("日报推送：推送任务仍在执行，跳过本轮触发, dueCrons={}", dueCrons);
            return;
        }
        try {
            if (!acquirePushDedupLock(now, timezone)) {
                log.info("日报推送：检测到同一分钟已推送，跳过本轮, dueCrons={}, timezone={}", dueCrons, timezone);
                return;
            }
            log.info("日报推送：开始执行，dueCrons={}, timezone={}", dueCrons, timezone);
            dailyReportService.pushDailyReport();
            log.info("日报推送：执行完成，dueCrons={}, timezone={}", dueCrons, timezone);
        } catch (Exception ex) {
            log.error("日报推送：执行异常，dueCrons={}, timezone={}", dueCrons, timezone, ex);
        } finally {
            Date recalculateBaseTime = new Date();
            for (String cron : dueCrons) {
                try {
                    Date next = nextExecutionTime(cron, recalculateBaseTime, timezone);
                    nextExecuteTimeMap.put(cron, next);
                } catch (Exception ex) {
                    log.error("日报推送：计算下一次触发时间失败，cron={}, timezone={}", cron, timezone, ex);
                    nextExecuteTimeMap.remove(cron);
                }
            }
            running.set(false);
        }
    }

    private boolean acquirePushDedupLock(Date now, String timezone) {
        String dedupKey = RedisKeyConstants.DAILY_REPORT_PUSH_DEDUP_PREFIX + formatMinuteBucket(now, timezone);
        String lockValue = String.valueOf(now.getTime());
        try {
            Boolean firstPush = stringRedisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, lockValue, 2, TimeUnit.HOURS);
            return Boolean.TRUE.equals(firstPush);
        } catch (Exception ex) {
            // Redis 异常时优先保障可用性，允许继续推送，避免日报完全丢失。
            log.error("日报推送：去重锁获取失败，将继续执行推送, dedupKey={}", dedupKey, ex);
            return true;
        }
    }

    private String formatMinuteBucket(Date now, String timezone) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        return formatter.format(now);
    }

    private Date nextExecutionTime(String cron, Date baseTime, String timezone) {
        SimpleTriggerContext triggerContext = new SimpleTriggerContext();
        triggerContext.update(baseTime, baseTime, baseTime);
        return new CronTrigger(cron, TimeZone.getTimeZone(timezone)).nextExecutionTime(triggerContext);
    }
}
