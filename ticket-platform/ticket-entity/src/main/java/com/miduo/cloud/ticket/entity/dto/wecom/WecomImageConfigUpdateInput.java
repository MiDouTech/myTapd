package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 企微图片处理配置更新请求 DTO
 * 接口编号：API000438
 */
@Data
public class WecomImageConfigUpdateInput implements Serializable {

    /**
     * 是否启用图片消息处理
     */
    private boolean enabled;

    /**
     * 关联时间窗口（分钟，范围 1-60）
     */
    @Min(value = 1, message = "关联时间窗口最小为1分钟")
    @Max(value = 60, message = "关联时间窗口最大为60分钟")
    private int associationWindowMinutes;

    /**
     * 超时处理策略（CREATE_TICKET / EXPIRE / NOTIFY_USER）
     */
    @NotBlank(message = "超时处理策略不能为空")
    private String timeoutStrategy;

    /**
     * 收到图片时是否立即回复提示
     */
    private boolean notifyOnPending;

    /**
     * 单工单最大图片数（1-50）
     */
    @Min(value = 1, message = "单工单最大图片数最小为1")
    @Max(value = 50, message = "单工单最大图片数最大为50")
    private int maxImagesPerTicket;
}
