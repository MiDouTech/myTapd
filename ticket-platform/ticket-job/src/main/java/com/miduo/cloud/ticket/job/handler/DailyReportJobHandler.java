package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.dailyreport.DailyReportApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日报自动推送定时任务
 * 默认每天 18:00 触发，推送到配置的企微群
 */
@Component
public class DailyReportJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DailyReportJobHandler.class);

    private final DailyReportApplicationService dailyReportService;

    public DailyReportJobHandler(DailyReportApplicationService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    /**
     * 每天 18:00 触发日报推送
     * Cron 表达式可通过 system_config 中的 daily_report_cron 动态控制（预留）
     * 定时任务本身固定 18:00 执行，服务内部会检查 enabled 开关
     */
    @Scheduled(cron = "0 0 18 * * ?")
    public void execute() {
        log.info("开始执行日报自动推送任务");
        try {
            if (!dailyReportService.isEnabled()) {
                log.info("日报自动推送已关闭，跳过本次执行");
                return;
            }
            dailyReportService.pushDailyReport();
        } catch (Exception ex) {
            log.error("日报自动推送任务执行异常", ex);
        }
        log.info("日报自动推送任务执行完成");
    }
}
