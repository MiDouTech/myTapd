package com.miduo.cloud.ticket.entity.dto.webhook;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Webhook配置响应对象
 */
@Data
public class WebhookConfigOutput implements Serializable {

    private Long id;

    private String url;

    private String secret;

    private List<String> eventTypes;

    private Integer isActive;

    private Integer timeoutMs;

    private Integer maxRetryTimes;

    private String description;

    private Date lastSuccessTime;

    private Date lastFailTime;

    private String lastFailReason;

    private Date createTime;

    private Date updateTime;
}
