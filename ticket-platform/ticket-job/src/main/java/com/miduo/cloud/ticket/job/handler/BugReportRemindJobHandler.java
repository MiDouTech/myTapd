package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.bugreport.BugReportApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bug简报超期催促任务
 */
@Component
public class BugReportRemindJobHandler {

    private static final Logger log = LoggerFactory.getLogger(BugReportRemindJobHandler.class);

    private final BugReportApplicationService bugReportApplicationService;

    public BugReportRemindJobHandler(BugReportApplicationService bugReportApplicationService) {
        this.bugReportApplicationService = bugReportApplicationService;
    }

    /**
     * 每日 09:00 触发一次超期提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void execute() {
        log.info("开始执行Bug简报超期催促任务");
        try {
            bugReportApplicationService.remindOverdueReports();
        } catch (Exception ex) {
            log.error("Bug简报超期催促任务执行异常", ex);
        }
        log.info("Bug简报超期催促任务执行完成");
    }
}
