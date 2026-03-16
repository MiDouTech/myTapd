package com.miduo.cloud.ticket.job.handler;

import com.miduo.cloud.ticket.application.wecom.WecomPendingImageCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 企微图片暂存超时清理定时任务
 * 每分钟扫描 wecom_pending_image 中超时未关联的 PENDING 图片，按策略处理
 * Task024：企微图片消息工单关联
 */
@Component
public class WecomPendingImageCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(WecomPendingImageCleanupJob.class);

    private final WecomPendingImageCleanupService cleanupService;

    public WecomPendingImageCleanupJob(WecomPendingImageCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    /**
     * 每分钟执行一次超时图片清理
     */
    @Scheduled(fixedRate = 60000)
    public void execute() {
        log.debug("开始执行企微图片暂存超时清理任务...");
        try {
            cleanupService.processExpiredPendingImages();
        } catch (Exception e) {
            log.error("企微图片暂存超时清理任务执行异常", e);
        }
        log.debug("企微图片暂存超时清理任务执行完成");
    }
}
