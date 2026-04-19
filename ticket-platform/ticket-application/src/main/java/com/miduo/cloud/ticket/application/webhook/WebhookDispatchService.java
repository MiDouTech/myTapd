package com.miduo.cloud.ticket.application.webhook;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.constants.AppConstants;
import com.miduo.cloud.ticket.common.enums.DispatchStrategy;
import com.miduo.cloud.ticket.common.enums.TicketAssignType;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.enums.WebhookDispatchStatus;
import com.miduo.cloud.ticket.common.enums.WebhookEventType;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
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
import java.time.DateTimeException;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

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
    private static final String BASIC_CONFIG_GROUP = "BASIC";
    private static final String BASIC_CONFIG_KEY_TIMEZONE = "timezone";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    @Value("${ticket.detail-url:}")
    private String ticketDetailUrl;

    private final WebhookConfigMapper webhookConfigMapper;
    private final WebhookDispatchLogMapper webhookDispatchLogMapper;
    private final TicketMapper ticketMapper;
    private final SysUserMapper sysUserMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final SystemConfigMapper systemConfigMapper;

    public WebhookDispatchService(WebhookConfigMapper webhookConfigMapper,
                                  WebhookDispatchLogMapper webhookDispatchLogMapper,
                                  TicketMapper ticketMapper,
                                  SysUserMapper sysUserMapper,
                                  TicketCategoryMapper ticketCategoryMapper,
                                  SystemConfigMapper systemConfigMapper) {
        this.webhookConfigMapper = webhookConfigMapper;
        this.webhookDispatchLogMapper = webhookDispatchLogMapper;
        this.ticketMapper = ticketMapper;
        this.sysUserMapper = sysUserMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.systemConfigMapper = systemConfigMapper;
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

    /**
     * Bug 简报归档：推送到已订阅 {@link WebhookEventType#BUG_REPORT_ARCHIVED} 的 Webhook（含企微机器人 URL）。
     *
     * @param ticketId 用于日志与负载中的工单快照；取关联工单中的主工单 ID（可为空则快照为空）
     */
    public void dispatchBugReportArchived(Long reportId,
                                          Long ticketId,
                                          String reportNo,
                                          List<Long> mentionedUserIds,
                                          String detailText) {
        BugReportArchivedWebhookPayload payload = new BugReportArchivedWebhookPayload();
        payload.setReportId(reportId);
        payload.setReportNo(reportNo);
        if (mentionedUserIds != null && !mentionedUserIds.isEmpty()) {
            payload.getMentionedUserIds().addAll(mentionedUserIds);
        }
        payload.setDetailText(detailText);
        dispatch(WebhookEventType.BUG_REPORT_ARCHIVED, ticketId, payload);
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
                continue;
            }
            // 企微群机器人 Webhook 常与工单事件共用一条 URL：未显式订阅简报归档时，若已订阅任一工单类事件则一并推送
            if (eventType == WebhookEventType.BUG_REPORT_ARCHIVED
                    && isWecomRobotWebhook(config.getUrl())
                    && containsAnyTicketLifecycleEvent(subscribedEventTypes)) {
                result.add(config);
            }
        }
        return result;
    }

    private boolean containsAnyTicketLifecycleEvent(Set<String> subscribedEventTypes) {
        if (subscribedEventTypes == null || subscribedEventTypes.isEmpty()) {
            return false;
        }
        return subscribedEventTypes.contains(WebhookEventType.TICKET_CREATED.getCode())
                || subscribedEventTypes.contains(WebhookEventType.TICKET_STATUS_CHANGED.getCode())
                || subscribedEventTypes.contains(WebhookEventType.TICKET_ASSIGNED.getCode())
                || subscribedEventTypes.contains(WebhookEventType.TICKET_COMPLETED.getCode())
                || subscribedEventTypes.contains(WebhookEventType.TICKET_CLOSED.getCode());
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
        if (ticket.getCreatorId() != null) {
            SysUserPO creator = sysUserMapper.selectById(ticket.getCreatorId());
            if (creator != null) {
                snapshot.setCreatorWecomUserid(creator.getWecomUserid());
                snapshot.setCreatorName(creator.getName());
            }
        }
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

        if (eventType == WebhookEventType.BUG_REPORT_ARCHIVED) {
            return buildWecomRobotBugReportArchivedPayload(ticket, eventTime, data);
        }

        StringBuilder content = new StringBuilder();
        content.append("【工单事件通知】\n");
        content.append("────────────────\n");
        content.append("【事件】")
                .append(eventName == null || eventName.isEmpty() ? eventType.getLabel() : eventName)
                .append("\n");
        if (eventTime != null && !eventTime.isEmpty()) {
            content.append("【时间】").append(eventTime).append("\n");
        }
        if (ticket != null) {
            content.append("【工单信息】\n");
            content.append("1) 工单编号：").append(safeJsonString(ticket, "ticketNo", "-")).append("\n");
            content.append("2) 标题：").append(safeJsonString(ticket, "title", "-")).append("\n");
            String statusCode = safeJsonString(ticket, "status", "-");
            content.append("3) 状态：").append(resolveStatusLabel(statusCode)).append("\n");
            String priorityCode = safeJsonString(ticket, "priority", "-");
            content.append("4) 优先级：").append(resolvePriorityDisplay(priorityCode)).append("\n");
            if (eventType == WebhookEventType.TICKET_STATUS_CHANGED && isPendingAcceptanceStatusCode(statusCode)) {
                Long assigneeId = ticket.getLong("assigneeId");
                content.append("5) 当前处理人：").append(resolveUserNameById(assigneeId)).append("\n");
            }
        }
        List<String> changeLines = buildChangeLines(data, eventType);
        if (!changeLines.isEmpty()) {
            content.append("【变更内容】\n");
            for (int i = 0; i < changeLines.size(); i++) {
                content.append(i + 1).append(") ").append(changeLines.get(i)).append("\n");
            }
            String changeTimeLine = (eventTime != null && !eventTime.isEmpty()) ? eventTime : formatNow();
            content.append("变更时间：").append(changeTimeLine).append("\n");
        }
        if (eventType == WebhookEventType.TICKET_COMMENT_MENTION) {
            appendCommentMentionSection(content, data);
        }
        String ticketNo = ticket != null ? safeJsonString(ticket, "ticketNo", null) : null;
        if (ticketNo != null && ticketDetailUrl != null && !ticketDetailUrl.trim().isEmpty()) {
            String baseUrl = ticketDetailUrl.trim().replaceAll("/$", "");
            content.append("【详情】").append(baseUrl).append("/").append(ticketNo);
        }

        MentionTargets mentionTargets = eventType == WebhookEventType.TICKET_COMMENT_MENTION
                ? collectCommentMentionTargets(data)
                : collectMentionTargets(ticket, data);

        String normalizedContent = truncateByUtf8Bytes(content.toString(), WECOM_TEXT_MAX_BYTES);
        JSONObject text = new JSONObject();
        text.put("content", normalizedContent);
        if (!mentionTargets.getWecomUserids().isEmpty()) {
            JSONArray mentionedList = new JSONArray();
            mentionedList.addAll(mentionTargets.getWecomUserids());
            text.put("mentioned_list", mentionedList);
        }
        if (!mentionTargets.getMobileList().isEmpty()) {
            JSONArray mentionedMobileList = new JSONArray();
            mentionedMobileList.addAll(mentionTargets.getMobileList());
            text.put("mentioned_mobile_list", mentionedMobileList);
        }

        JSONObject payload = new JSONObject();
        payload.put("msgtype", "text");
        payload.put("text", text);
        return JSON.toJSONString(payload);
    }

    /**
     * Bug 简报归档：与工单通知相同的企微机器人 text 形态，便于复用已配置的工单 Webhook URL。
     */
    private String buildWecomRobotBugReportArchivedPayload(JSONObject ticket, String eventTime, Object data) {
        JSONObject d = null;
        try {
            if (data != null) {
                d = JSON.parseObject(JSON.toJSONString(data));
            }
        } catch (Exception ex) {
            log.warn("解析Bug简报归档Webhook data失败: {}", ex.getMessage());
        }
        String reportNo = "-";
        if (d != null) {
            String rn = d.getString("reportNo");
            if (rn != null && !rn.trim().isEmpty()) {
                reportNo = rn.trim();
            }
        }
        String detail = "";
        if (d != null) {
            String dt = d.getString("detailText");
            if (dt != null) {
                detail = dt;
            }
        }

        StringBuilder content = new StringBuilder();
        content.append("【Bug简报归档】\n");
        content.append("────────────────\n");
        content.append("【事件】Bug简报归档\n");
        if (eventTime != null && !eventTime.isEmpty()) {
            content.append("【时间】").append(eventTime).append("\n");
        }
        content.append("【简报编号】").append(reportNo).append("\n");
        if (ticket != null) {
            content.append("【关联工单】\n");
            content.append("1) 工单编号：").append(safeJsonString(ticket, "ticketNo", "-")).append("\n");
            content.append("2) 标题：").append(safeJsonString(ticket, "title", "-")).append("\n");
            String statusCode = safeJsonString(ticket, "status", "-");
            content.append("3) 状态：").append(resolveStatusLabel(statusCode)).append("\n");
            String priorityCode = safeJsonString(ticket, "priority", "-");
            content.append("4) 优先级：").append(resolvePriorityDisplay(priorityCode)).append("\n");
        }
        if (!detail.trim().isEmpty()) {
            content.append("【简报摘要】\n").append(detail.trim());
            if (!detail.endsWith("\n")) {
                content.append("\n");
            }
        }
        String ticketNo = ticket != null ? safeJsonString(ticket, "ticketNo", null) : null;
        if (ticketNo != null && ticketDetailUrl != null && !ticketDetailUrl.trim().isEmpty()) {
            String baseUrl = ticketDetailUrl.trim().replaceAll("/$", "");
            content.append("【详情】").append(baseUrl).append("/").append(ticketNo);
        }

        MentionTargets mentionTargets = collectBugReportArchivedMentionTargets(ticket, d);

        String normalizedContent = truncateByUtf8Bytes(content.toString(), WECOM_TEXT_MAX_BYTES);
        JSONObject text = new JSONObject();
        text.put("content", normalizedContent);
        if (!mentionTargets.getWecomUserids().isEmpty()) {
            JSONArray mentionedList = new JSONArray();
            mentionedList.addAll(mentionTargets.getWecomUserids());
            text.put("mentioned_list", mentionedList);
        }
        if (!mentionTargets.getMobileList().isEmpty()) {
            JSONArray mentionedMobileList = new JSONArray();
            mentionedMobileList.addAll(mentionTargets.getMobileList());
            text.put("mentioned_mobile_list", mentionedMobileList);
        }

        JSONObject payload = new JSONObject();
        payload.put("msgtype", "text");
        payload.put("text", text);
        return JSON.toJSONString(payload);
    }

    private MentionTargets collectBugReportArchivedMentionTargets(JSONObject ticket, JSONObject data) {
        MentionTargets targets = new MentionTargets();
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        if (data != null && data.containsKey("mentionedUserIds")) {
            JSONArray arr = data.getJSONArray("mentionedUserIds");
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    Long uid = arr.getLong(i);
                    if (uid != null) {
                        userIds.add(uid);
                    }
                }
            }
        }
        if (ticket != null) {
            String creatorWecomUserid = normalizeWecomUserid(safeJsonString(ticket, "creatorWecomUserid", null));
            if (creatorWecomUserid != null) {
                targets.getWecomUserids().add(creatorWecomUserid);
            }
            Long creatorId = ticket.getLong("creatorId");
            if (creatorId != null) {
                userIds.add(creatorId);
            }
            Long assigneeId = ticket.getLong("assigneeId");
            if (assigneeId != null) {
                userIds.add(assigneeId);
            }
        }
        if (userIds.isEmpty()) {
            return targets;
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(new ArrayList<>(userIds));
        if (users == null || users.isEmpty()) {
            return targets;
        }
        for (SysUserPO user : users) {
            if (user == null) {
                continue;
            }
            String wecomUserid = normalizeWecomUserid(user.getWecomUserid());
            if (wecomUserid != null) {
                targets.getWecomUserids().add(wecomUserid);
                continue;
            }
            String normalizedMobile = normalizeMentionMobile(user.getPhone());
            if (normalizedMobile != null) {
                targets.getMobileList().add(normalizedMobile);
            }
        }
        return targets;
    }

    /**
     * 收集需要@的目标：优先企微 userId，同时补充手机号兜底。
     * 这么做是为了处理“系统里有处理人，但没同步到 wecom_userid”导致的漏@问题。
     */
    private void appendCommentMentionSection(StringBuilder content, Object data) {
        if (data == null) {
            return;
        }
        try {
            JSONObject json = JSON.parseObject(JSON.toJSONString(data));
            if (json == null) {
                return;
            }
            content.append("【评论@】\n");
            Long authorId = json.getLong("commentAuthorUserId");
            content.append("评论人：").append(resolveUserNameById(authorId)).append("\n");
            String summary = json.getString("commentPlainSummary");
            if (summary == null || summary.trim().isEmpty()) {
                summary = "（无文本摘要）";
            }
            content.append("摘要：").append(summary.trim()).append("\n");
            JSONArray mentioned = json.getJSONArray("mentionedUserIds");
            if (mentioned != null && !mentioned.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (int i = 0; i < mentioned.size(); i++) {
                    Long uid = mentioned.getLong(i);
                    if (uid != null) {
                        names.add(resolveUserNameById(uid));
                    }
                }
                content.append("被@：").append(String.join("、", names)).append("\n");
            }
        } catch (Exception ex) {
            log.warn("组装评论@Webhook正文失败: {}", ex.getMessage());
        }
    }

    /**
     * 评论@事件：仅 @ 被提及用户（企微机器人 mentioned_list / mentioned_mobile_list）
     */
    private MentionTargets collectCommentMentionTargets(Object data) {
        MentionTargets targets = new MentionTargets();
        if (data == null) {
            return targets;
        }
        try {
            JSONObject json = JSON.parseObject(JSON.toJSONString(data));
            if (json == null) {
                return targets;
            }
            JSONArray arr = json.getJSONArray("mentionedUserIds");
            if (arr == null || arr.isEmpty()) {
                return targets;
            }
            Set<Long> userIds = new LinkedHashSet<>();
            for (int i = 0; i < arr.size(); i++) {
                Long id = arr.getLong(i);
                if (id != null && id > 0) {
                    userIds.add(id);
                }
            }
            if (userIds.isEmpty()) {
                return targets;
            }
            List<SysUserPO> users = sysUserMapper.selectBatchIds(new ArrayList<>(userIds));
            if (users == null || users.isEmpty()) {
                return targets;
            }
            for (SysUserPO user : users) {
                if (user == null) {
                    continue;
                }
                String wecomUserid = normalizeWecomUserid(user.getWecomUserid());
                if (wecomUserid != null) {
                    targets.getWecomUserids().add(wecomUserid);
                    continue;
                }
                String normalizedMobile = normalizeMentionMobile(user.getPhone());
                if (normalizedMobile != null) {
                    targets.getMobileList().add(normalizedMobile);
                }
            }
        } catch (Exception ex) {
            log.warn("收集评论@Webhook mention 失败: {}", ex.getMessage());
        }
        return targets;
    }

    private MentionTargets collectMentionTargets(JSONObject ticket, Object data) {
        MentionTargets targets = new MentionTargets();
        Set<Long> userIds = new LinkedHashSet<>();
        if (ticket != null) {
            // 保留快照内已携带的企微账号，避免用户记录缺失时丢失@对象。
            String creatorWecomUserid = normalizeWecomUserid(safeJsonString(ticket, "creatorWecomUserid", null));
            if (creatorWecomUserid != null) {
                targets.getWecomUserids().add(creatorWecomUserid);
            }
            Long creatorId = ticket.getLong("creatorId");
            if (creatorId != null) {
                userIds.add(creatorId);
            }
            Long assigneeId = ticket.getLong("assigneeId");
            if (assigneeId != null) {
                userIds.add(assigneeId);
            }
        }
        if (data != null) {
            try {
                JSONObject json = JSON.parseObject(JSON.toJSONString(data));
                if (json != null) {
                    List<Long> assigneeIds = parseLongArrayFromJson(json, "assigneeIds");
                    userIds.addAll(assigneeIds);
                    Long assigneeId = json.getLong("assigneeId");
                    if (assigneeId != null) {
                        userIds.add(assigneeId);
                    }
                    Long previousAssigneeId = json.getLong("previousAssigneeId");
                    if (previousAssigneeId != null) {
                        userIds.add(previousAssigneeId);
                    }
                    Long operatorId = json.getLong("operatorId");
                    if (operatorId != null) {
                        userIds.add(operatorId);
                    }
                }
            } catch (Exception ex) {
                log.warn("收集@mention用户失败: {}", ex.getMessage());
            }
        }
        if (userIds.isEmpty()) {
            return targets;
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(new ArrayList<>(userIds));
        if (users == null || users.isEmpty()) {
            return targets;
        }
        for (SysUserPO user : users) {
            if (user == null) {
                continue;
            }
            String wecomUserid = normalizeWecomUserid(user.getWecomUserid());
            if (wecomUserid != null) {
                targets.getWecomUserids().add(wecomUserid);
                continue;
            }
            String normalizedMobile = normalizeMentionMobile(user.getPhone());
            if (normalizedMobile != null) {
                targets.getMobileList().add(normalizedMobile);
            }
        }
        return targets;
    }

    private List<Long> parseLongArrayFromJson(JSONObject json, String key) {
        if (json == null || key == null || key.trim().isEmpty() || !json.containsKey(key)) {
            return new ArrayList<>();
        }
        JSONArray arr = json.getJSONArray(key);
        if (arr == null || arr.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> values = new LinkedHashSet<>();
        for (int i = 0; i < arr.size(); i++) {
            Long value = arr.getLong(i);
            if (value != null) {
                values.add(value);
            }
        }
        return new ArrayList<>(values);
    }

    private String normalizeWecomUserid(String wecomUserid) {
        if (wecomUserid == null) {
            return null;
        }
        String normalized = wecomUserid.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private String normalizeMentionMobile(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String digitsOnly = phone.trim().replaceAll("[^0-9+]", "");
        if (digitsOnly.startsWith("+86")) {
            digitsOnly = digitsOnly.substring(3);
        } else if (digitsOnly.startsWith("86") && digitsOnly.length() > 11) {
            digitsOnly = digitsOnly.substring(2);
        }
        if (digitsOnly.isEmpty()) {
            return null;
        }
        if (!digitsOnly.matches("[0-9]{5,20}")) {
            return null;
        }
        return digitsOnly;
    }

    private String resolveStatusLabel(String code) {
        if (code == null || "-".equals(code)) {
            return "-";
        }
        TicketStatus status = TicketStatus.fromCode(code);
        return status != null ? status.getLabel() : code;
    }

    private boolean isPendingAcceptanceStatusCode(String code) {
        if (code == null || code.trim().isEmpty() || "-".equals(code)) {
            return false;
        }
        TicketStatus status = TicketStatus.fromCode(code);
        return status != null && status.isPendingAcceptanceLike();
    }

    private String resolvePriorityLabel(String code) {
        if (code == null || "-".equals(code)) {
            return "-";
        }
        Priority priority = Priority.fromCode(code);
        return priority != null ? priority.getLabel() : code;
    }

    private String resolvePriorityDisplay(String code) {
        if (code == null || "-".equals(code)) {
            return "-";
        }
        Priority priority = Priority.fromCode(code);
        if (priority == null) {
            return code;
        }
        switch (priority) {
            case URGENT:
                return "🔴 " + priority.getLabel();
            case HIGH:
                return "🟠 " + priority.getLabel();
            case MEDIUM:
                return "🟡 " + priority.getLabel();
            case LOW:
                return "🟢 " + priority.getLabel();
            default:
                return priority.getLabel();
        }
    }

    private List<String> buildChangeLines(Object data, WebhookEventType eventType) {
        List<String> changeLines = new ArrayList<>();
        if (data == null) {
            return changeLines;
        }
        try {
            JSONObject json = JSON.parseObject(JSON.toJSONString(data));
            if (json == null || json.isEmpty()) {
                return changeLines;
            }
            if (json.containsKey("categoryId")) {
                Long categoryId = json.getLong("categoryId");
                String categoryName = resolveCategoryName(categoryId);
                changeLines.add("分类：" + categoryName);
            }
            if (json.containsKey("priority")) {
                changeLines.add("优先级：" + resolvePriorityDisplay(json.getString("priority")));
            }
            if (json.containsKey("oldStatus")) {
                changeLines.add("原状态：" + resolveStatusLabel(json.getString("oldStatus")));
            }
            if (json.containsKey("newStatus")) {
                changeLines.add("新状态：" + resolveStatusLabel(json.getString("newStatus")));
            }
            if (json.containsKey("assigneeId")) {
                List<Long> assigneeIds = parseLongArrayFromJson(json, "assigneeIds");
                if (eventType == WebhookEventType.TICKET_ASSIGNED && !assigneeIds.isEmpty()) {
                    changeLines.add("指派给：" + resolveUserNamesByIds(assigneeIds));
                } else {
                    Long assigneeId = json.getLong("assigneeId");
                    changeLines.add("指派给：" + resolveUserNameById(assigneeId));
                }
            }
            if (json.containsKey("assignType")) {
                changeLines.add("分派类型：" + resolveAssignTypeLabel(json.getString("assignType")));
            }
            if (json.containsKey("operatorId")) {
                Long operatorId = json.getLong("operatorId");
                changeLines.add("操作人：" + resolveUserNameById(operatorId));
            }
            if (json.containsKey("previousAssigneeId")) {
                Long prevId = json.getLong("previousAssigneeId");
                if (prevId != null) {
                    changeLines.add("原处理人：" + resolveUserNameById(prevId));
                }
            }
            if (json.containsKey("commentPlainSummary")) {
                String s = json.getString("commentPlainSummary");
                changeLines.add("评论摘要：" + (s != null && !s.trim().isEmpty() ? s.trim() : "（空）"));
            }
            if (json.containsKey("commentAuthorUserId")) {
                changeLines.add("评论人：" + resolveUserNameById(json.getLong("commentAuthorUserId")));
            }
            if (json.containsKey("mentionedUserIds")) {
                JSONArray arr = json.getJSONArray("mentionedUserIds");
                if (arr != null && !arr.isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        Long uid = arr.getLong(i);
                        if (uid != null) {
                            names.add(resolveUserNameById(uid));
                        }
                    }
                    changeLines.add("被@用户：" + String.join("、", names));
                }
            }
            return changeLines;
        } catch (Exception ex) {
            log.warn("解析变更数据失败: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    private String resolveUserNamesByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return "-";
        }
        Set<Long> deduped = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                deduped.add(userId);
            }
        }
        if (deduped.isEmpty()) {
            return "-";
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(new ArrayList<>(deduped));
        if (users == null || users.isEmpty()) {
            return "-";
        }
        java.util.Map<Long, String> nameMap = new java.util.HashMap<>();
        for (SysUserPO user : users) {
            if (user != null && user.getId() != null && user.getName() != null && !user.getName().trim().isEmpty()) {
                nameMap.put(user.getId(), user.getName().trim());
            }
        }
        StringBuilder names = new StringBuilder();
        for (Long userId : deduped) {
            String name = nameMap.get(userId);
            if (name == null) {
                continue;
            }
            if (names.length() > 0) {
                names.append("、");
            }
            names.append(name);
        }
        return names.length() > 0 ? names.toString() : "-";
    }

    private String resolveUserNameById(Long userId) {
        if (userId == null) {
            return "-";
        }
        SysUserPO user = sysUserMapper.selectById(userId);
        if (user == null || user.getName() == null) {
            return "-";
        }
        return user.getName();
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "-";
        }
        TicketCategoryPO category = ticketCategoryMapper.selectById(categoryId);
        if (category == null || category.getName() == null) {
            return "-";
        }
        return category.getName();
    }

    private String resolveAssignTypeLabel(String assignType) {
        if (assignType == null || assignType.trim().isEmpty()) {
            return "-";
        }
        TicketAssignType reason = TicketAssignType.fromCode(assignType);
        if (reason != null) {
            return reason.getLabel();
        }
        DispatchStrategy strategy = DispatchStrategy.fromCode(assignType);
        if (strategy != null) {
            return strategy.getLabel();
        }
        return assignType;
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
        sdf.setTimeZone(TimeZone.getTimeZone(getWebhookDisplayTimezone()));
        return sdf.format(new Date());
    }

    private String getWebhookDisplayTimezone() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, BASIC_CONFIG_GROUP)
                .eq(SystemConfigPO::getConfigKey, BASIC_CONFIG_KEY_TIMEZONE)
                .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO timezoneConfig = systemConfigMapper.selectOne(wrapper);
        if (timezoneConfig == null || timezoneConfig.getConfigValue() == null || timezoneConfig.getConfigValue().trim().isEmpty()) {
            return DEFAULT_TIMEZONE;
        }
        String timezone = timezoneConfig.getConfigValue().trim();
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (DateTimeException ex) {
            log.warn("Webhook推送：读取到非法时区配置，timezone={}，回退默认时区={}", timezone, DEFAULT_TIMEZONE);
            return DEFAULT_TIMEZONE;
        }
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
        private String creatorName;
        private String creatorWecomUserid;
        private Long assigneeId;
    }

    @Data
    private static class MentionTargets {
        private final Set<String> wecomUserids = new LinkedHashSet<>();
        private final Set<String> mobileList = new LinkedHashSet<>();
    }

    @Data
    private static class HttpPostResult {
        private int responseCode;
        private String responseBody;
    }
}
