package com.miduo.cloud.ticket.application.webhook;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.constants.AppConstants;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.enums.WebhookDispatchStatus;
import com.miduo.cloud.ticket.common.enums.WebhookEventType;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.mapper.WebhookConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.mapper.WebhookDispatchLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po.WebhookConfigPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po.WebhookDispatchLogPO;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
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
    private static final int WECOM_TEXT_MAX_BYTES = 1900;
    private static final int MAX_REQUEST_BODY_LENGTH = 8000;
    private static final int MAX_RESPONSE_BODY_LENGTH = 4000;
    private static final int MAX_FAIL_REASON_LENGTH = 900;

    @Value("${ticket.detail-url:http://ticket.t.miduonet.com/ticket/detail}")
    private String ticketDetailUrl;

    private final WebhookConfigMapper webhookConfigMapper;
    private final WebhookDispatchLogMapper webhookDispatchLogMapper;
    private final TicketMapper ticketMapper;

    public WebhookDispatchService(WebhookConfigMapper webhookConfigMapper,
                                  WebhookDispatchLogMapper webhookDispatchLogMapper,
                                  TicketMapper ticketMapper) {
        this.webhookConfigMapper = webhookConfigMapper;
        this.webhookDispatchLogMapper = webhookDispatchLogMapper;
        this.ticketMapper = ticketMapper;
    }

    public void dispatch(WebhookEventType eventType, Long ticketId, Object eventData) {
        if (eventType == null) {
            return;
        }
        List<WebhookConfigPO> configs = webhookConfigMapper.selectAllActive();
        if (configs == null || configs.isEmpty()) {
            log.debug("Webhook分发跳过：无启用配置, eventType={}, ticketId={}", eventType.getCode(), ticketId);
            saveSkippedDispatchLog(eventType, ticketId, "无启用Webhook配置");
            return;
        }
        List<WebhookConfigPO> subscribedConfigs = filterSubscribedConfigs(configs, eventType);
        if (subscribedConfigs.isEmpty()) {
            log.debug("Webhook分发跳过：无配置订阅该事件, eventType={}, ticketId={}", eventType.getCode(), ticketId);
            saveSkippedDispatchLog(eventType, ticketId, "无配置订阅该事件");
            return;
        }
        log.info("Webhook事件分发开始: eventType={}, ticketId={}, subscriberCount={}",
                eventType.getCode(), ticketId, subscribedConfigs.size());

        TicketSnapshot snapshot = buildTicketSnapshot(ticketId);
        WebhookPayload payload = buildPayload(eventType, ticketId, snapshot, eventData);
        String payloadJson = JSON.toJSONString(payload);

        for (WebhookConfigPO config : subscribedConfigs) {
            pushToWebhook(config, eventType, ticketId, payloadJson);
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

    private void pushToWebhook(WebhookConfigPO config, WebhookEventType eventType, Long ticketId, String payloadJson) {
        int retryTimes = config.getMaxRetryTimes() == null ? 0 : Math.max(config.getMaxRetryTimes(), 0);
        boolean success = false;
        String failReason = null;
        String safeUrl = sanitizeWebhookUrl(config.getUrl());
        String outboundPayloadJson = resolveOutboundPayload(config, eventType, ticketId, payloadJson);

        for (int i = 0; i <= retryTimes; i++) {
            int attempt = i + 1;
            long startMs = System.currentTimeMillis();
            Integer responseCode = null;
            String responseBody = null;
            String currentFailReason = null;
            boolean currentSuccess = false;
            try {
                log.debug("Webhook推送尝试: configId={}, eventType={}, attempt={}/{}, url={}",
                        config.getId(), eventType.getCode(), attempt, retryTimes + 1, safeUrl);
                HttpPostResult result = doHttpPost(config, eventType, outboundPayloadJson);
                responseCode = result.getResponseCode();
                responseBody = result.getResponseBody();
                if (result.getResponseCode() >= 200 && result.getResponseCode() < 300) {
                    success = true;
                    currentSuccess = true;
                    log.info("Webhook推送成功: configId={}, eventType={}, attempt={}/{}, url={}, responseCode={}",
                            config.getId(), eventType.getCode(), attempt, retryTimes + 1, safeUrl, result.getResponseCode());
                    break;
                }
                String responseBodySummary = summarize(result.getResponseBody(), 300);
                currentFailReason = "HTTP " + result.getResponseCode()
                        + (responseBodySummary == null ? "" : " body=" + responseBodySummary);
                failReason = currentFailReason;
                log.warn("Webhook推送响应非2xx: configId={}, eventType={}, attempt={}/{}, url={}, responseCode={}, responseBody={}",
                        config.getId(), eventType.getCode(), attempt, retryTimes + 1, safeUrl,
                        result.getResponseCode(), responseBodySummary);
            } catch (Exception ex) {
                currentFailReason = ex.getMessage();
                failReason = currentFailReason;
                log.warn("Webhook推送异常: configId={}, eventType={}, attempt={}/{}, url={}, reason={}",
                        config.getId(), eventType.getCode(), attempt, retryTimes + 1, safeUrl, ex.getMessage(), ex);
            } finally {
                long durationMs = System.currentTimeMillis() - startMs;
                saveDispatchLog(config, eventType, ticketId, safeUrl, outboundPayloadJson, attempt, retryTimes + 1,
                        currentSuccess ? WebhookDispatchStatus.SUCCESS : WebhookDispatchStatus.FAIL,
                        responseCode, responseBody, currentFailReason, durationMs);
            }
        }

        Date now = new Date();
        if (success) {
            config.setLastSuccessTime(now);
            config.setLastFailReason(null);
            config.setLastFailTime(null);
        } else {
            config.setLastFailTime(now);
            config.setLastFailReason(summarize(failReason, MAX_FAIL_REASON_LENGTH));
            log.error("Webhook推送最终失败: configId={}, eventType={}, url={}, reason={}",
                    config.getId(), eventType.getCode(), safeUrl, summarize(failReason, 300));
        }
        webhookConfigMapper.updateById(config);
    }

    private void saveSkippedDispatchLog(WebhookEventType eventType, Long ticketId, String reason) {
        saveDispatchLog(null, eventType, ticketId, null, null, 0, 0, WebhookDispatchStatus.SKIPPED,
                null, null, reason, null);
    }

    private void saveDispatchLog(WebhookConfigPO config,
                                 WebhookEventType eventType,
                                 Long ticketId,
                                 String requestUrl,
                                 String requestBody,
                                 Integer attemptNo,
                                 Integer maxAttempts,
                                 WebhookDispatchStatus status,
                                 Integer responseCode,
                                 String responseBody,
                                 String failReason,
                                 Long durationMs) {
        try {
            WebhookDispatchLogPO logPO = new WebhookDispatchLogPO();
            logPO.setWebhookConfigId(config == null ? null : config.getId());
            logPO.setEventType(eventType == null ? null : eventType.getCode());
            logPO.setTicketId(ticketId);
            logPO.setRequestUrl(summarize(requestUrl, 500));
            logPO.setRequestBody(summarize(requestBody, MAX_REQUEST_BODY_LENGTH));
            logPO.setAttemptNo(attemptNo);
            logPO.setMaxAttempts(maxAttempts);
            logPO.setStatus(status == null ? null : status.getCode());
            logPO.setResponseCode(responseCode);
            logPO.setResponseBody(summarize(responseBody, MAX_RESPONSE_BODY_LENGTH));
            logPO.setFailReason(summarize(failReason, MAX_FAIL_REASON_LENGTH));
            logPO.setDurationMs(durationMs);
            logPO.setDispatchTime(new Date());
            webhookDispatchLogMapper.insert(logPO);
        } catch (Exception ex) {
            log.error("保存Webhook分发日志失败: eventType={}, ticketId={}, configId={}, reason={}",
                    eventType == null ? null : eventType.getCode(),
                    ticketId,
                    config == null ? null : config.getId(),
                    ex.getMessage(), ex);
        }
    }

    private String resolveOutboundPayload(WebhookConfigPO config,
                                          WebhookEventType eventType,
                                          Long ticketId,
                                          String payloadJson) {
        if (config == null || config.getUrl() == null || config.getUrl().trim().isEmpty()) {
            return payloadJson;
        }
        if (!isWecomRobotWebhook(config.getUrl())) {
            return payloadJson;
        }
        return buildWecomRobotTextPayload(eventType, ticketId, payloadJson);
    }

    private boolean isWecomRobotWebhook(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("qyapi.weixin.qq.com/cgi-bin/webhook/send");
    }

    /**
     * 适配企业微信机器人Webhook格式，避免返回40008（invalid message type）。
     * 文档：https://developer.work.weixin.qq.com/document/path/91770
     */
    private String buildWecomRobotTextPayload(WebhookEventType eventType, Long ticketId, String rawPayloadJson) {
        JSONObject sourcePayload = null;
        try {
            sourcePayload = JSON.parseObject(rawPayloadJson);
        } catch (Exception ex) {
            log.warn("解析Webhook原始负载失败，企微消息将降级输出简版文本: {}", ex.getMessage());
        }

        String eventName = safeJsonString(sourcePayload, "eventName");
        String eventTime = safeJsonString(sourcePayload, "eventTime");
        JSONObject ticket = sourcePayload == null ? null : sourcePayload.getJSONObject("ticket");
        Object data = sourcePayload == null ? null : sourcePayload.get("data");

        StringBuilder content = new StringBuilder();
        content.append("【工单事件通知】\n");
        content.append("事件：")
                .append(eventName == null || eventName.isEmpty() ? eventType.getCode() : eventName)
                .append(" (").append(eventType.getCode()).append(")\n");
        if (eventTime != null && !eventTime.isEmpty()) {
            content.append("时间：").append(eventTime).append("\n");
        }
        if (ticket != null) {
            content.append("工单编号：").append(safeJsonString(ticket, "ticketNo", "-")).append("\n");
            content.append("标题：").append(safeJsonString(ticket, "title", "-")).append("\n");
            String statusCode = safeJsonString(ticket, "status", "-");
            content.append("状态：").append(resolveStatusLabel(statusCode)).append("\n");
            String priorityCode = safeJsonString(ticket, "priority", "-");
            content.append("优先级：").append(resolvePriorityLabel(priorityCode)).append("\n");
        }
        if (data != null) {
            String changeSummary = buildChangeSummary(data);
            if (changeSummary != null && !changeSummary.trim().isEmpty()) {
                content.append("变更：").append(changeSummary).append("\n");
            }
        }
        if (ticketId != null && ticketDetailUrl != null && !ticketDetailUrl.trim().isEmpty()) {
            String baseUrl = ticketDetailUrl.trim().replaceAll("/$", "");
            content.append("详情：").append(baseUrl).append("/").append(ticketId);
        }

        String normalizedContent = truncateByUtf8Bytes(content.toString(), WECOM_TEXT_MAX_BYTES);
        JSONObject text = new JSONObject();
        text.put("content", normalizedContent);

        JSONObject payload = new JSONObject();
        payload.put("msgtype", "text");
        payload.put("text", text);
        return JSON.toJSONString(payload);
    }

    private String resolveStatusLabel(String code) {
        if (code == null || "-".equals(code)) {
            return "-";
        }
        TicketStatus status = TicketStatus.fromCode(code);
        return status != null ? status.getLabel() : code;
    }

    private String resolvePriorityLabel(String code) {
        if (code == null || "-".equals(code)) {
            return "-";
        }
        Priority priority = Priority.fromCode(code);
        return priority != null ? priority.getLabel() : code;
    }

    private String buildChangeSummary(Object data) {
        if (data == null) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(JSON.toJSONString(data));
            if (json == null || json.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            if (json.containsKey("categoryId")) {
                sb.append("分类ID: ").append(json.get("categoryId"));
            }
            if (json.containsKey("priority")) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("优先级: ").append(resolvePriorityLabel(json.getString("priority")));
            }
            if (json.containsKey("oldStatus")) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("原状态: ").append(resolveStatusLabel(json.getString("oldStatus")));
            }
            if (json.containsKey("newStatus")) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("新状态: ").append(resolveStatusLabel(json.getString("newStatus")));
            }
            if (json.containsKey("assigneeId")) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("指派给: ").append(json.get("assigneeId"));
            }
            if (json.containsKey("assignType")) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("分派类型: ").append(json.getString("assignType"));
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception ex) {
            log.warn("解析变更数据失败: {}", ex.getMessage());
            return null;
        }
    }

    private String safeJsonString(JSONObject jsonObject, String key) {
        return safeJsonString(jsonObject, key, null);
    }

    private String safeJsonString(JSONObject jsonObject, String key, String defaultValue) {
        if (jsonObject == null || key == null || key.trim().isEmpty()) {
            return defaultValue;
        }
        String value = jsonObject.getString(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String truncateByUtf8Bytes(String value, int maxBytes) {
        if (value == null) {
            return "";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return value;
        }
        String suffix = "...(内容已截断)";
        int suffixBytes = suffix.getBytes(StandardCharsets.UTF_8).length;
        int allowBytes = Math.max(0, maxBytes - suffixBytes);
        int currentBytes = 0;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            byte[] charBytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
            if (currentBytes + charBytes.length > allowBytes) {
                break;
            }
            result.append(c);
            currentBytes += charBytes.length;
        }
        result.append(suffix);
        return result.toString();
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
