package com.miduo.cloud.ticket.entity.dto.webhook;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Webhook配置创建请求
 */
@Data
public class WebhookConfigCreateInput implements Serializable {

    @NotBlank(message = "Webhook地址不能为空")
    private String url;

    private String secret;

    @NotEmpty(message = "事件类型不能为空")
    private List<String> eventTypes;

    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "启用状态只能为0或1")
    @Max(value = 1, message = "启用状态只能为0或1")
    private Integer isActive;

    @NotNull(message = "超时时间不能为空")
    @Min(value = 1000, message = "超时时间不能小于1000毫秒")
    @Max(value = 60000, message = "超时时间不能超过60000毫秒")
    private Integer timeoutMs;

    @NotNull(message = "重试次数不能为空")
    @Min(value = 0, message = "重试次数不能小于0")
    @Max(value = 5, message = "重试次数不能超过5")
    private Integer maxRetryTimes;

    private String description;
}
