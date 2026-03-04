package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.application.user.WecomSyncService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.wecom.SyncLogPageInput;
import com.miduo.cloud.ticket.entity.dto.wecom.SyncManualOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.SyncStatusOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SysSyncLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SysWeworkConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SysSyncLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SysWeworkConfigPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 企业微信同步应用服务
 */
@Service
public class WecomSyncApplicationService extends BaseApplicationService {

    private static final String SYNC_TYPE_FULL = "FULL";
    private static final String SYNC_MODE_MANUAL = "MANUAL";
    private static final String SYNC_MODE_SCHEDULE = "SCHEDULE";
    private static final String SYNC_STATUS_SUCCESS = "SUCCESS";
    private static final String SYNC_STATUS_FAILED = "FAILED";
    private static final String SYNC_STATUS_PARTIAL = "PARTIAL";
    private static final int DEFAULT_MAX_RETRY_COUNT = 0;
    private static final long RETRY_BACKOFF_BASE_MS = 1000L;

    private final WecomSyncService wecomSyncService;
    private final SysSyncLogMapper sysSyncLogMapper;
    private final SysWeworkConfigMapper sysWeworkConfigMapper;

    public WecomSyncApplicationService(WecomSyncService wecomSyncService,
                                       SysSyncLogMapper sysSyncLogMapper,
                                       SysWeworkConfigMapper sysWeworkConfigMapper) {
        this.wecomSyncService = wecomSyncService;
        this.sysSyncLogMapper = sysSyncLogMapper;
        this.sysWeworkConfigMapper = sysWeworkConfigMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public SyncManualOutput manualSync(String triggerBy) {
        return executeAndLog(SYNC_MODE_MANUAL, triggerBy);
    }

    @Transactional(rollbackFor = Exception.class)
    public void scheduleSync() {
        executeAndLog(SYNC_MODE_SCHEDULE, null);
    }

    public SyncStatusOutput latestStatus() {
        SysSyncLogPO latest = sysSyncLogMapper.selectOne(
                new LambdaQueryWrapper<SysSyncLogPO>()
                        .orderByDesc(SysSyncLogPO::getCreateTime)
                        .orderByDesc(SysSyncLogPO::getId)
                        .last("limit 1")
        );
        if (latest == null) {
            return null;
        }
        return toStatusOutput(latest);
    }

    public PageOutput<SyncStatusOutput> pageLogs(SyncLogPageInput input) {
        Page<SysSyncLogPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        LambdaQueryWrapper<SysSyncLogPO> wrapper = new LambdaQueryWrapper<>();
        if (input.getSyncMode() != null && !input.getSyncMode().trim().isEmpty()) {
            wrapper.eq(SysSyncLogPO::getSyncMode, input.getSyncMode().trim());
        }
        if (input.getSyncStatus() != null && !input.getSyncStatus().trim().isEmpty()) {
            wrapper.eq(SysSyncLogPO::getSyncStatus, input.getSyncStatus().trim());
        }
        wrapper.orderByDesc(SysSyncLogPO::getCreateTime)
                .orderByDesc(SysSyncLogPO::getId);
        Page<SysSyncLogPO> result = sysSyncLogMapper.selectPage(page, wrapper);
        List<SyncStatusOutput> records = result.getRecords().stream()
                .map(this::toStatusOutput)
                .collect(Collectors.toList());
        return PageOutput.of(records, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    public ScheduleConfig getScheduleConfig() {
        SysWeworkConfigPO config = getLatestEnabledConfig();
        ScheduleConfig output = new ScheduleConfig();
        if (config == null) {
            output.setScheduleEnabled(false);
            output.setScheduleCron(null);
            return output;
        }
        output.setScheduleEnabled(config.getScheduleEnabled() != null && config.getScheduleEnabled() == 1);
        output.setScheduleCron(config.getScheduleCron());
        return output;
    }

    private SyncManualOutput executeAndLog(String syncMode, String triggerBy) {
        Date executeStartTime = new Date();
        int retryCount = 0;
        int maxRetryCount = getMaxRetryCount();
        try {
            WecomSyncService.SyncResult result = null;
            while (retryCount <= maxRetryCount) {
                try {
                    result = wecomSyncService.syncAllWithResult();
                    break;
                } catch (Exception ex) {
                    if (retryCount >= maxRetryCount) {
                        throw ex;
                    }
                    retryCount++;
                    sleepForRetry(retryCount);
                }
            }
            if (result == null) {
                throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "同步失败: 同步结果为空");
            }

            SysSyncLogPO logPO = new SysSyncLogPO();
            logPO.setSyncType(SYNC_TYPE_FULL);
            logPO.setSyncMode(syncMode);
            logPO.setSyncStatus(resolveSyncStatus(result.getFailCount()));
            logPO.setTotalCount(result.getTotalCount());
            logPO.setSuccessCount(result.getSuccessCount());
            logPO.setFailCount(result.getFailCount());
            logPO.setRetryCount(retryCount);
            logPO.setDurationMs(result.getEndTime().getTime() - result.getStartTime().getTime());
            logPO.setTriggerBy(triggerBy);
            logPO.setErrorMessage(result.getErrorMessage());
            logPO.setStartTime(result.getStartTime());
            logPO.setEndTime(result.getEndTime());
            sysSyncLogMapper.insert(logPO);

            SyncManualOutput output = new SyncManualOutput();
            output.setSyncStatus(logPO.getSyncStatus());
            output.setTotalCount(logPO.getTotalCount());
            output.setSuccessCount(logPO.getSuccessCount());
            output.setFailCount(logPO.getFailCount());
            output.setErrorMessage(logPO.getErrorMessage());
            output.setStartTime(logPO.getStartTime());
            output.setEndTime(logPO.getEndTime());
            output.setDurationMs(logPO.getDurationMs());
            return output;
        } catch (Exception ex) {
            SysSyncLogPO failedLog = new SysSyncLogPO();
            failedLog.setSyncType(SYNC_TYPE_FULL);
            failedLog.setSyncMode(syncMode);
            failedLog.setSyncStatus(SYNC_STATUS_FAILED);
            failedLog.setTotalCount(0);
            failedLog.setSuccessCount(0);
            failedLog.setFailCount(0);
            failedLog.setRetryCount(retryCount);
            failedLog.setTriggerBy(triggerBy);
            failedLog.setErrorMessage(ex.getMessage());
            Date executeEndTime = new Date();
            failedLog.setStartTime(executeStartTime);
            failedLog.setEndTime(executeEndTime);
            failedLog.setDurationMs(Math.max(executeEndTime.getTime() - executeStartTime.getTime(), 0L));
            sysSyncLogMapper.insert(failedLog);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "同步失败: " + ex.getMessage());
        }
    }

    private int getMaxRetryCount() {
        SysWeworkConfigPO config = getLatestEnabledConfig();
        if (config == null || config.getRetryCount() == null || config.getRetryCount() < 0) {
            return DEFAULT_MAX_RETRY_COUNT;
        }
        return config.getRetryCount();
    }

    private SysWeworkConfigPO getLatestEnabledConfig() {
        return sysWeworkConfigMapper.selectOne(
                new LambdaQueryWrapper<SysWeworkConfigPO>()
                        .eq(SysWeworkConfigPO::getStatus, 1)
                        .orderByDesc(SysWeworkConfigPO::getUpdateTime)
                        .orderByDesc(SysWeworkConfigPO::getId)
                        .last("limit 1")
        );
    }

    private void sleepForRetry(int retryIndex) {
        long delayMs = RETRY_BACKOFF_BASE_MS * retryIndex;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "同步重试被中断");
        }
    }

    private String resolveSyncStatus(Integer failCount) {
        if (failCount == null || failCount == 0) {
            return SYNC_STATUS_SUCCESS;
        }
        return SYNC_STATUS_PARTIAL;
    }

    private SyncStatusOutput toStatusOutput(SysSyncLogPO po) {
        SyncStatusOutput output = new SyncStatusOutput();
        output.setSyncType(po.getSyncType());
        output.setSyncMode(po.getSyncMode());
        output.setSyncStatus(po.getSyncStatus());
        output.setTotalCount(po.getTotalCount());
        output.setSuccessCount(po.getSuccessCount());
        output.setFailCount(po.getFailCount());
        output.setRetryCount(po.getRetryCount());
        output.setTriggerBy(po.getTriggerBy());
        output.setErrorMessage(po.getErrorMessage());
        output.setStartTime(po.getStartTime());
        output.setEndTime(po.getEndTime());
        output.setDurationMs(po.getDurationMs());
        return output;
    }

    @lombok.Data
    public static class ScheduleConfig {
        private Boolean scheduleEnabled;
        private String scheduleCron;
    }
}
