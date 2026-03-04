package com.miduo.cloud.ticket.entity.dto.webhook;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Webhook推送日志分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookDispatchLogPageInput extends PageInput {

    private Long webhookConfigId;

    private Long ticketId;

    private String eventType;

    private String status;
}
