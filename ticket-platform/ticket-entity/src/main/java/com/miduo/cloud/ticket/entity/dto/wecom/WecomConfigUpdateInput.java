package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 企业微信配置更新入参
 */
@Data
public class WecomConfigUpdateInput implements Serializable {

    @NotBlank(message = "corpId不能为空")
    @Size(max = 100, message = "corpId长度不能超过100")
    private String corpId;

    @NotBlank(message = "agentId不能为空")
    @Size(max = 50, message = "agentId长度不能超过50")
    private String agentId;

    @NotBlank(message = "corpSecret不能为空")
    @Size(max = 255, message = "corpSecret长度不能超过255")
    private String corpSecret;

    @NotBlank(message = "apiBaseUrl不能为空")
    @Size(max = 255, message = "apiBaseUrl长度不能超过255")
    private String apiBaseUrl;

    @NotNull(message = "connectTimeoutMs不能为空")
    @Min(value = 1000, message = "connectTimeoutMs不能小于1000")
    @Max(value = 120000, message = "connectTimeoutMs不能大于120000")
    private Integer connectTimeoutMs;

    @NotNull(message = "readTimeoutMs不能为空")
    @Min(value = 1000, message = "readTimeoutMs不能小于1000")
    @Max(value = 180000, message = "readTimeoutMs不能大于180000")
    private Integer readTimeoutMs;

    @NotNull(message = "scheduleEnabled不能为空")
    private Boolean scheduleEnabled;

    @Size(max = 64, message = "scheduleCron长度不能超过64")
    private String scheduleCron;

    @NotNull(message = "retryCount不能为空")
    @Min(value = 0, message = "retryCount不能小于0")
    @Max(value = 10, message = "retryCount不能大于10")
    private Integer retryCount;

    @NotNull(message = "batchSize不能为空")
    @Min(value = 1, message = "batchSize不能小于1")
    @Max(value = 1000, message = "batchSize不能大于1000")
    private Integer batchSize;

    private Boolean enabled;

    private String callbackToken;

    private String callbackAesKey;
}
