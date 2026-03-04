package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企业微信配置出参
 */
@Data
public class WecomConfigOutput implements Serializable {

    private Long id;
    private String corpId;
    private String agentId;
    private String corpSecretMasked;
    private String apiBaseUrl;
    private Integer connectTimeoutMs;
    private Integer readTimeoutMs;
    private Boolean scheduleEnabled;
    private String scheduleCron;
    private Integer retryCount;
    private Integer batchSize;
    private Boolean enabled;
    private Date updateTime;
}
