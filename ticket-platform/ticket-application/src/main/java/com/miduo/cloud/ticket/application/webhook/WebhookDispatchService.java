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
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
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
        String safeUrl = sanitizeWebhookUrl(config.getUrl());

        for (int i = 0; i <= retryTimes; i++) {
            int attempt = i + 1;
            try {
                log.debug("Webhook推送尝试: configId={}, eventType={}, attempt={}/{}, url={}",
                        config.getId(), eventType.getCode(), attempt, retryTimes + 1, safeUrl);
                HttpPostResult result = doHttpPost(config, eventType, payloadJson);
                if (result.getResponseCode() >= 200 && result.getResponseCode() < 300) {
                    success = true;
                    log.info("Webhook推送成功: configId={}, eventType={}, attempt={}/{}, url={}, responseCode={}",
                            config.getId(), eventType.getCode(), attempt, retryTimes + 1, safeUrl, result.getResponseCode());
                    break;
                }
                String responseBodySummary = summarize(result.getResponseBody(), 300);
                failReason = "HTTP " + result.getResponseCode()
                        + (responseBodySummary == null ? "" : " body=" + responseBodySummary);
                log.warn("Webhook推送响应非2xx: configId={}, eventType={}, attempt={}/{}, url={}, responseCode={}, responseBody={}",
                        config.getId(), eventType.getCode(), attempt, retryTimes + 1, safeUrl,
                        result.getResponseCode(), responseBodySummary);
            } catch (Exception ex) {
                failReason = ex.getMessage();
                log.warn("Webhook推送异常: configId={}, eventType={}, attempt={}/{}, url={}, reason={}",
                        config.getId(), eventType.getCode(), attempt, retryTimes + 1, safeUrl, ex.getMessage(), ex);
            }
        }

        Date now = new Date();
        if (success) {
            config.setLastSuccessTime(now);
            config.setLastFailReason(null);
            config.setLastFailTime(null);
        } else {
            config.setLastFailTime(now);
            config.setLastFailReason(summarize(failReason, 900));
            log.error("Webhook推送最终失败: configId={}, eventType={}, url={}, reason={}",
                    config.getId(), eventType.getCode(), safeUrl, summarize(failReason, 300));
        }
        webhookConfigMapper.updateById(config);
    }

    private HttpPostResult doHttpPost(WebhookConfigPO config,
                                      WebhookEventType eventType,
                                      String payloadJson) throws Exception {
        URL url = new URL(config.getUrl());
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
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
            int responseCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, responseCode);

            HttpPostResult result = new HttpPostResult();
            result.setResponseCode(responseCode);
            result.setResponseBody(responseBody);
            return result;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponseBody(HttpURLConnection connection, int responseCode) {
        InputStream inputStream = null;
        try {
            if (responseCode >= 200 && responseCode < 400) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }
            if (inputStream == null) {
                return null;
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.debug("读取Webhook响应体失败: {}", ex.getMessage());
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String formatNow() {
        SimpleDateFormat sdf = new SimpleDateFormat(AppConstants.DATETIME_FORMAT);
        return sdf.format(new Date());
    }

    private String sanitizeWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return "";
        }
        String normalized = webhookUrl.trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            return normalized.substring(0, queryIndex) + "?***";
        }
        return normalized;
    }

    private String summarize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
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

    @Data
    private static class HttpPostResult {
        private int responseCode;
        private String responseBody;
    }
}
