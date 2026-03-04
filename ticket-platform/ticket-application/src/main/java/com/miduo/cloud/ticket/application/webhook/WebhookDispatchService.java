package com.miduo.cloud.ticket.application.webhook;

import com.alibaba.fastjson2.JSON;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.constants.AppConstants;
import com.miduo.cloud.ticket.common.enums.WebhookEventType;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.mapper.WebhookConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po.WebhookConfigPO;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Webhook事件推送服务
 */
@Service
public class WebhookDispatchService extends BaseApplicationService {

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final WebhookConfigMapper webhookConfigMapper;
    private final TicketMapper ticketMapper;

    public WebhookDispatchService(WebhookConfigMapper webhookConfigMapper,
                                  TicketMapper ticketMapper) {
        this.webhookConfigMapper = webhookConfigMapper;
        this.ticketMapper = ticketMapper;
    }

    public void dispatch(WebhookEventType eventType, Long ticketId, Object eventData) {
        if (eventType == null) {
            return;
        }
        List<WebhookConfigPO> configs = webhookConfigMapper.selectAllActive();
        if (configs == null || configs.isEmpty()) {
            log.debug("Webhook分发跳过：无启用配置, eventType={}, ticketId={}", eventType.getCode(), ticketId);
            return;
        }
        List<WebhookConfigPO> subscribedConfigs = filterSubscribedConfigs(configs, eventType);
        if (subscribedConfigs.isEmpty()) {
            log.debug("Webhook分发跳过：无配置订阅该事件, eventType={}, ticketId={}", eventType.getCode(), ticketId);
            return;
        }
        log.info("Webhook事件分发开始: eventType={}, ticketId={}, subscriberCount={}",
                eventType.getCode(), ticketId, subscribedConfigs.size());

        TicketSnapshot snapshot = buildTicketSnapshot(ticketId);
        WebhookPayload payload = buildPayload(eventType, ticketId, snapshot, eventData);
        String payloadJson = JSON.toJSONString(payload);

        for (WebhookConfigPO config : subscribedConfigs) {
            pushToWebhook(config, eventType, payloadJson);
        }
    }

    private List<WebhookConfigPO> filterSubscribedConfigs(List<WebhookConfigPO> configs, WebhookEventType eventType) {
        if (configs == null || configs.isEmpty() || eventType == null) {
            return new ArrayList<>();
        }
        String targetEventType = normalizeEventCode(eventType.getCode());
        List<WebhookConfigPO> result = new ArrayList<>();
        for (WebhookConfigPO config : configs) {
            Set<String> subscribedEventTypes = parseEventTypes(config.getEventTypes());
            if (subscribedEventTypes.contains(targetEventType)) {
                result.add(config);
            }
        }
        return result;
    }

    /**
     * 兼容历史数据格式：支持CSV和JSON数组两种存储结构。
     */
    private Set<String> parseEventTypes(String rawEventTypes) {
        Set<String> result = new LinkedHashSet<>();
        if (rawEventTypes == null || rawEventTypes.trim().isEmpty()) {
            return result;
        }
        String normalized = rawEventTypes.trim();

        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            try {
                List<String> jsonEventTypes = JSON.parseArray(normalized, String.class);
                if (jsonEventTypes != null) {
                    for (String item : jsonEventTypes) {
                        String code = normalizeEventCode(item);
                        if (code != null) {
                            result.add(code);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("解析Webhook事件类型JSON失败，将回退CSV解析: rawEventTypes={}", rawEventTypes, ex);
            }
        }

        if (!result.isEmpty()) {
            return result;
        }

        String[] parts = normalized.split("[,，]");
        for (String part : parts) {
            String code = normalizeEventCode(part);
            if (code != null) {
                result.add(code);
            }
        }
        return result;
    }

    private String normalizeEventCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private TicketSnapshot buildTicketSnapshot(Long ticketId) {
        if (ticketId == null) {
            return null;
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return null;
        }
        TicketSnapshot snapshot = new TicketSnapshot();
        snapshot.setId(ticket.getId());
        snapshot.setTicketNo(ticket.getTicketNo());
        snapshot.setTitle(ticket.getTitle());
        snapshot.setStatus(ticket.getStatus());
        snapshot.setPriority(ticket.getPriority());
        snapshot.setCreatorId(ticket.getCreatorId());
        snapshot.setAssigneeId(ticket.getAssigneeId());
        return snapshot;
    }

    private WebhookPayload buildPayload(WebhookEventType eventType,
                                        Long ticketId,
                                        TicketSnapshot snapshot,
                                        Object eventData) {
        WebhookPayload payload = new WebhookPayload();
        payload.setEventType(eventType.getCode());
        payload.setEventName(eventType.getLabel());
        payload.setEventTime(formatNow());
        payload.setTicketId(ticketId);
        payload.setTicket(snapshot);
        payload.setData(eventData);
        return payload;
    }

    private void pushToWebhook(WebhookConfigPO config, WebhookEventType eventType, String payloadJson) {
        int retryTimes = config.getMaxRetryTimes() == null ? 0 : Math.max(config.getMaxRetryTimes(), 0);
        boolean success = false;
        String failReason = null;

        for (int i = 0; i <= retryTimes; i++) {
            try {
                int responseCode = doHttpPost(config, eventType, payloadJson);
                if (responseCode >= 200 && responseCode < 300) {
                    success = true;
                    break;
                }
                failReason = "HTTP " + responseCode;
            } catch (Exception ex) {
                failReason = ex.getMessage();
                log.warn("Webhook推送失败: configId={}, attempt={}, reason={}",
                        config.getId(), i + 1, ex.getMessage());
            }
        }

        Date now = new Date();
        if (success) {
            config.setLastSuccessTime(now);
            config.setLastFailReason(null);
        } else {
            config.setLastFailTime(now);
            config.setLastFailReason(failReason);
        }
        webhookConfigMapper.updateById(config);
    }

    private int doHttpPost(WebhookConfigPO config,
                           WebhookEventType eventType,
                           String payloadJson) throws Exception {
        URL url = new URL(config.getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        connection.setRequestProperty("X-Webhook-Event", eventType.getCode());
        if (config.getSecret() != null && !config.getSecret().trim().isEmpty()) {
            connection.setRequestProperty("X-Webhook-Secret", config.getSecret().trim());
        }
        int timeoutMs = config.getTimeoutMs() == null ? DEFAULT_TIMEOUT_MS : Math.max(config.getTimeoutMs(), 1000);
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);

        byte[] bytes = payloadJson.getBytes(StandardCharsets.UTF_8);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(bytes);
            outputStream.flush();
        }
        return connection.getResponseCode();
    }

    private String formatNow() {
        SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATETIME_FORMAT);
        return sdf.format(new Date());
    }

    @Data
    private static class WebhookPayload {
        private String eventType;
        private String eventName;
        private String eventTime;
        private Long ticketId;
        private TicketSnapshot ticket;
        private Object data;
    }

    @Data
    private static class TicketSnapshot {
        private Long id;
        private String ticketNo;
        private String title;
        private String status;
        private String priority;
        private Long creatorId;
        private Long assigneeId;
    }
}
