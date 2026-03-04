package com.miduo.cloud.ticket.entity.dto.webhook;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Webhook推送日志响应对象
 */
@Data
public class WebhookDispatchLogOutput implements Serializable {

    private Long id;

    private Long webhookConfigId;

    private String eventType;

    private Long ticketId;

    private String requestUrl;

    private Integer attemptNo;

    private Integer maxAttempts;

    private String status;

    private Integer responseCode;

    private String failReason;

    private Long durationMs;

    private Date dispatchTime;

    private Date createTime;
}
