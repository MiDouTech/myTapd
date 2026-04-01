package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警事件处理结果枚举
 */
@Getter
@AllArgsConstructor
public enum AlertProcessResult {

    CREATED("CREATED", "已创建工单"),
    DEDUP("DEDUP", "去重跳过"),
    RECOVERED("RECOVERED", "恢复事件"),
    UNMAPPED("UNMAPPED", "无映射配置-使用默认"),
    ERROR("ERROR", "处理异常");

    private final String code;
    private final String label;
}
