package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Webhook推送明细日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("webhook_dispatch_log")
public class WebhookDispatchLogPO extends BaseEntity {

    @TableField("webhook_config_id")
    private Long webhookConfigId;

    @TableField("event_type")
    private String eventType;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("request_url")
    private String requestUrl;

    @TableField("request_body")
    private String requestBody;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("max_attempts")
    private Integer maxAttempts;

    @TableField("status")
    private String status;

    @TableField("response_code")
    private Integer responseCode;

    @TableField("response_body")
    private String responseBody;

    @TableField("fail_reason")
    private String failReason;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("dispatch_time")
    private Date dispatchTime;
}
