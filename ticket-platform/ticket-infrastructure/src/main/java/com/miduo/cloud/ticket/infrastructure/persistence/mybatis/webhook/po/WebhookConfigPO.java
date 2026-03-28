package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Webhook配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("webhook_config")
public class WebhookConfigPO extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("url")
    private String url;

    @TableField("secret")
    private String secret;

    @TableField("event_types")
    private String eventTypes;

    @TableField("is_active")
    private Integer isActive;

    @TableField("timeout_ms")
    private Integer timeoutMs;

    @TableField("max_retry_times")
    private Integer maxRetryTimes;

    @TableField("description")
    private String description;

    @TableField("last_success_time")
    private Date lastSuccessTime;

    @TableField("last_fail_time")
    private Date lastFailTime;

    @TableField("last_fail_reason")
    private String lastFailReason;
}
