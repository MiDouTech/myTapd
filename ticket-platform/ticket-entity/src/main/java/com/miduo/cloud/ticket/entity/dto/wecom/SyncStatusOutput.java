package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 最近同步状态
 */
@Data
public class SyncStatusOutput implements Serializable {

    private String syncType;
    private String syncMode;
    private String syncStatus;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer retryCount;
    private String triggerBy;
    private String errorMessage;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
}
