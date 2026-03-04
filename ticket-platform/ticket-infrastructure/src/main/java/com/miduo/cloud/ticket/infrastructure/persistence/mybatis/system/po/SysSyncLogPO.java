package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 同步日志持久化对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sync_log")
public class SysSyncLogPO extends BaseEntity {

    @TableField("sync_type")
    private String syncType;

    @TableField("sync_mode")
    private String syncMode;

    @TableField("sync_status")
    private String syncStatus;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("success_count")
    private Integer successCount;

    @TableField("fail_count")
    private Integer failCount;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("trigger_by")
    private String triggerBy;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("start_time")
    private Date startTime;

    @TableField("end_time")
    private Date endTime;
}
