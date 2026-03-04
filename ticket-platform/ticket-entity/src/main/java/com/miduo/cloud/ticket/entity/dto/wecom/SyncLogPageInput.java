package com.miduo.cloud.ticket.entity.dto.wecom;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 同步日志分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SyncLogPageInput extends PageInput {

    /**
     * 同步模式（MANUAL/SCHEDULE）
     */
    private String syncMode;

    /**
     * 同步状态（SUCCESS/PARTIAL/FAILED）
     */
    private String syncStatus;
}
