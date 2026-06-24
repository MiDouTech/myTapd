package com.miduo.cloud.ticket.application.plugin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.miduo.cloud.ticket.application.integration.IntegrationAppCredentialResolver;
import com.miduo.cloud.ticket.common.enums.WebhookEventType;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po.IntegrationAppPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件接入应用按应用回调 Webhook
 */
@Service
public class PluginIntegrationWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PluginIntegrationWebhookService.class);
    private static final int TIMEOUT_MS = 5000;

    private final TicketMapper ticketMapper;
    private final IntegrationAppCredentialResolver credentialResolver;

    public PluginIntegrationWebhookService(TicketMapper ticketMapper,
                                           IntegrationAppCredentialResolver credentialResolver) {
        this.ticketMapper = ticketMapper;
        this.credentialResolver = credentialResolver;
    }

    public void dispatchIfNeeded(List<WebhookEventType> eventTypes, Long ticketId, Object eventData) {
        if (ticketId == null || eventTypes == null || eventTypes.isEmpty()) {
            return;
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || ticket.getIntegrationAppId() == null) {
            return;
        }
        IntegrationAppPO app = credentialResolver.requireEnabledApp(ticket.getIntegrationAppId());
        if (app == null || !StringUtils.hasText(app.getCallbackUrl())) {
            return;
        }
        WebhookEventType primaryEventType = eventTypes.get(0);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", primaryEventType.getCode());
        payload.put("eventName", primaryEventType.getLabel());
        payload.put("eventTime", new Date());
        payload.put("ticketNo", ticket.getTicketNo());
        payload.put("externalTicketRef", ticket.getExternalTicketRef());
        payload.put("status", ticket.getStatus());
        if (eventData instanceof Map) {
            payload.put("data", eventData);
        } else if (eventData != null) {
            payload.put("data", JSON.parseObject(JSON.toJSONString(eventData)));
        }
        Map<String, Object> ticketSummary = new LinkedHashMap<>();
        ticketSummary.put("id", ticket.getId());
        ticketSummary.put("title", ticket.getTitle());
        ticketSummary.put("status", ticket.getStatus());
        ticketSummary.put("priority", ticket.getPriority());
        payload.put("ticket", ticketSummary);
        if (StringUtils.hasText(ticket.getPluginContext())) {
            try {
                JSONObject context = JSON.parseObject(ticket.getPluginContext());
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("system", context.getString("system"));
                summary.put("module", context.getString("module"));
                summary.put("bizId", context.getString("bizId"));
                summary.put("bizType", context.getString("bizType"));
                if (context.getJSONObject("user") != null) {
                    summary.put("user", context.getJSONObject("user"));
                }
                payload.put("pluginContext", summary);
            } catch (Exception ex) {
                log.warn("插件Webhook上下文摘要失败: ticketId={}", ticketId, ex);
            }
        }
        String body = JSON.toJSONString(payload);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(app.getCallbackUrl().trim()).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("X-Webhook-Event", primaryEventType.getCode());
            connection.setRequestProperty("X-App-Key", app.getAppKey());
            if (StringUtils.hasText(app.getCallbackSecret())) {
                connection.setRequestProperty("X-Webhook-Signature", sign(body, app.getCallbackSecret()));
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
            int statusCode = connection.getResponseCode();
            log.info("插件Webhook推送完成: appKey={}, ticketNo={}, statusCode={}",
                    app.getAppKey(), ticket.getTicketNo(), statusCode);
        } catch (Exception ex) {
            log.warn("插件Webhook推送失败: appKey={}, ticketNo={}, error={}",
                    app.getAppKey(), ticket.getTicketNo(), ex.getMessage());
        }
    }

    private String sign(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("插件Webhook签名失败", ex);
        }
    }
}
