package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.io.Serializable;

/**
 * 企微图片处理配置输出 DTO
 * 接口编号：API000437
 */
@Data
public class WecomImageConfigOutput implements Serializable {

    /**
     * 是否启用图片消息处理
     */
    private boolean enabled;

    /**
     * 关联时间窗口（分钟）
     */
    private int associationWindowMinutes;

    /**
     * 超时处理策略（CREATE_TICKET / EXPIRE / NOTIFY_USER）
     */
    private String timeoutStrategy;

    /**
     * 收到图片时是否立即回复提示
     */
    private boolean notifyOnPending;

    /**
     * 单工单最大图片数
     */
    private int maxImagesPerTicket;
}
