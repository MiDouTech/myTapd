package com.miduo.cloud.ticket.application.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.ticket.entity.dto.alert.NightingaleAlertEvent;
import com.miduo.cloud.ticket.entity.dto.alert.NightingaleNotifyUser;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 将夜莺「HTTP 通知媒介」自定义请求体（含根级 {@code notify_users}、内嵌 {@code event}）
 * 规范为 {@link NightingaleAlertEvent}，与原生 AlertCurEvent 直传兼容。
 */
@Component
public class AlertWebhookPayloadAdapter {

    private static final DateTimeFormatter[] TRIGGER_TIME_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    };

    private final ObjectMapper objectMapper;

    public AlertWebhookPayloadAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析告警 Webhook 原始 JSON。
     * <ul>
     *   <li>含对象字段 {@code event}：以内层为夜莺事件，根级字段做补充（尤其 {@code notify_users} → notify_users_obj）</li>
     *   <li>含夜莺 {@code hash}：按原生 AlertCurEvent 反序列化，若根级仍有 {@code notify_users} 则合并</li>
     *   <li>否则视为仅根级字段的精简体，补 {@code hash} 供去重</li>
     * </ul>
     */
    public NightingaleAlertEvent parse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);
        NightingaleAlertEvent event;
        if (root.hasNonNull("event") && root.get("event").isObject()) {
            event = objectMapper.treeToValue(root.get("event"), NightingaleAlertEvent.class);
        } else if (root.hasNonNull("hash")) {
            event = objectMapper.treeToValue(root, NightingaleAlertEvent.class);
        } else {
            event = new NightingaleAlertEvent();
        }
        applyHttpNotifyMerge(root, event);
        ensureHash(event);
        return event;
    }

    private void applyHttpNotifyMerge(JsonNode root, NightingaleAlertEvent event) throws Exception {
        if (root.has("notify_users") && root.get("notify_users").isArray() && root.get("notify_users").size() > 0) {
            List<NightingaleNotifyUser> users = new ArrayList<>();
            for (JsonNode u : root.get("notify_users")) {
                users.add(objectMapper.treeToValue(u, NightingaleNotifyUser.class));
            }
            event.setNotifyUsersObj(users);
        }
        if (root.has("rule_name") && root.get("rule_name").isTextual()) {
            String v = root.get("rule_name").asText();
            if (StringUtils.hasText(v)) {
                event.setRuleName(v);
            }
        }
        JsonNode sev = root.get("severity");
        if (sev != null && !sev.isNull() && !sev.isMissingNode()) {
            Integer parsed = parseSeverityNode(sev);
            if (parsed != null) {
                event.setSeverity(parsed);
            }
        }
        if (root.has("status") && root.get("status").isTextual()) {
            String s = root.get("status").asText();
            if ("recovered".equalsIgnoreCase(s)) {
                event.setIsRecovered(true);
            } else if ("firing".equalsIgnoreCase(s)) {
                event.setIsRecovered(false);
            }
        }
        if (root.has("trigger_time") && root.get("trigger_time").isTextual()) {
            String tt = root.get("trigger_time").asText();
            if (StringUtils.hasText(tt)) {
                Long epoch = parseTriggerTimeToEpochSeconds(tt.trim());
                if (epoch != null && (event.getTriggerTime() == null || event.getTriggerTime() == 0L)) {
                    event.setTriggerTime(epoch);
                }
            }
        }
    }

    private Integer parseSeverityNode(JsonNode sev) {
        if (sev.isNumber()) {
            return sev.intValue();
        }
        if (sev.isTextual()) {
            String t = sev.asText().trim();
            if (t.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(t);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long parseTriggerTimeToEpochSeconds(String tt) {
        if (tt.matches("^-?\\d+$")) {
            long n = Long.parseLong(tt);
            if (n > 1_000_000_000_000L) {
                return n / 1000;
            }
            return n;
        }
        for (DateTimeFormatter f : TRIGGER_TIME_FORMATTERS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(tt, f);
                return ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
            } catch (DateTimeParseException ignored) {
                // 尝试下一种格式
            }
        }
        return null;
    }

    private void ensureHash(NightingaleAlertEvent event) {
        if (StringUtils.hasText(event.getHash())) {
            return;
        }
        String rule = event.getRuleName() != null ? event.getRuleName() : "";
        String target = event.getTargetIdent() != null ? event.getTargetIdent() : "";
        long tt = event.getTriggerTime() != null ? event.getTriggerTime() : 0L;
        String rec = Boolean.TRUE.equals(event.getIsRecovered()) ? "1" : "0";
        String basis = rule + "|" + target + "|" + tt + "|" + rec;
        event.setHash(DigestUtils.md5DigestAsHex(basis.getBytes(StandardCharsets.UTF_8)));
    }
}
