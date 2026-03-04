package com.miduo.cloud.ticket.application.webhook;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookDispatchLogOutput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookDispatchLogPageInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.mapper.WebhookDispatchLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po.WebhookDispatchLogPO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Webhook推送日志应用服务
 */
@Service
public class WebhookDispatchLogApplicationService extends BaseApplicationService {

    private final WebhookDispatchLogMapper webhookDispatchLogMapper;

    public WebhookDispatchLogApplicationService(WebhookDispatchLogMapper webhookDispatchLogMapper) {
        this.webhookDispatchLogMapper = webhookDispatchLogMapper;
    }

    public PageOutput<WebhookDispatchLogOutput> page(WebhookDispatchLogPageInput input) {
        Page<WebhookDispatchLogPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        QueryWrapper<WebhookDispatchLogPO> wrapper = new QueryWrapper<>();
        buildPageCondition(wrapper, input);
        wrapper.orderByDesc("dispatch_time").orderByDesc("id");

        Page<WebhookDispatchLogPO> result = webhookDispatchLogMapper.selectPage(page, wrapper);
        List<WebhookDispatchLogOutput> outputs = result.getRecords().stream()
                .map(this::convertToOutput)
                .collect(Collectors.toList());
        return PageOutput.of(outputs, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    private void buildPageCondition(QueryWrapper<WebhookDispatchLogPO> wrapper, WebhookDispatchLogPageInput input) {
        if (input.getWebhookConfigId() != null) {
            wrapper.eq("webhook_config_id", input.getWebhookConfigId());
        }
        if (input.getTicketId() != null) {
            wrapper.eq("ticket_id", input.getTicketId());
        }
        if (input.getEventType() != null && !input.getEventType().trim().isEmpty()) {
            wrapper.eq("event_type", input.getEventType().trim());
        }
        if (input.getStatus() != null && !input.getStatus().trim().isEmpty()) {
            wrapper.eq("status", input.getStatus().trim());
        }
    }

    private WebhookDispatchLogOutput convertToOutput(WebhookDispatchLogPO po) {
        WebhookDispatchLogOutput output = new WebhookDispatchLogOutput();
        output.setId(po.getId());
        output.setWebhookConfigId(po.getWebhookConfigId());
        output.setEventType(po.getEventType());
        output.setTicketId(po.getTicketId());
        output.setRequestUrl(po.getRequestUrl());
        output.setAttemptNo(po.getAttemptNo());
        output.setMaxAttempts(po.getMaxAttempts());
        output.setStatus(po.getStatus());
        output.setResponseCode(po.getResponseCode());
        output.setFailReason(po.getFailReason());
        output.setDurationMs(po.getDurationMs());
        output.setDispatchTime(po.getDispatchTime());
        output.setCreateTime(po.getCreateTime());
        return output;
    }
}
