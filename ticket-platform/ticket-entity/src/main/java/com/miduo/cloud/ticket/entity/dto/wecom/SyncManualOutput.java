package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 手动同步结果
 */
@Data
public class SyncManualOutput implements Serializable {

    private String syncStatus;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer departmentCreatedCount;
    private Integer departmentUpdatedCount;
    private Integer departmentDisabledCount;
    private Integer userCreatedCount;
    private Integer userUpdatedCount;
    private Integer userDisabledCount;
    private String errorMessage;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
}
