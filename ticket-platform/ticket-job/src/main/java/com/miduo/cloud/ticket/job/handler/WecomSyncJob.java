package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.user.WecomSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 企微通讯录同步定时任务
 * 每天凌晨1点全量同步部门与成员数据
 */
@Component
public class WecomSyncJob {

    private static final Logger log = LoggerFactory.getLogger(WecomSyncJob.class);

    private final WecomSyncService wecomSyncService;

    public WecomSyncJob(WecomSyncService wecomSyncService) {
        this.wecomSyncService = wecomSyncService;
    }

    /**
     * 每天凌晨1:00执行全量同步
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void syncWecomContacts() {
        log.info("定时任务：开始企微通讯录全量同步...");
        try {
            wecomSyncService.syncAll();
            log.info("定时任务：企微通讯录全量同步完成");
        } catch (Exception e) {
            log.error("定时任务：企微通讯录全量同步失败", e);
        }
    }
}
