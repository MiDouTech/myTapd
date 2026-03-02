package com.miduo.cloud.ticket.entity.dto.webhook;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Webhook配置分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookConfigPageInput extends PageInput {

    private String keyword;

    private String eventType;

    private Integer isActive;
}
