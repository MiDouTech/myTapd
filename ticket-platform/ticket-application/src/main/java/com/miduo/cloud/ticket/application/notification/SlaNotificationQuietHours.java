package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SLA 通知静默时段（默认 22:00 至次日 09:00，不含 09:00）
 */
@Component
public class SlaNotificationQuietHours {

    private static final Logger log = LoggerFactory.getLogger(SlaNotificationQuietHours.class);

    private static final String GROUP_SLA = "SLA";
    private static final String KEY_QUIET_START = "sla_notify_quiet_start";
    private static final String KEY_QUIET_END = "sla_notify_quiet_end";
    private static final String KEY_TIMEZONE = "timezone";
    private static final String DEFAULT_QUIET_START = "22:00";
    private static final String DEFAULT_QUIET_END = "09:00";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private final SystemConfigMapper systemConfigMapper;

    public SlaNotificationQuietHours(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    /**
     * 当前是否处于静默时段（晚 22:00 至次日 9:00 前）
     */
    public boolean isQuietHours(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        QuietHoursConfig config = loadConfig();
        LocalTime time = dateTime.toLocalTime();
        LocalTime quietStart = config.getQuietStart();
        LocalTime quietEnd = config.getQuietEnd();

        // 跨天区间：[22:00, 24:00) ∪ [00:00, 09:00)
        return !time.isBefore(quietStart) || time.isBefore(quietEnd);
    }

    /**
     * 若处于静默期，返回下一次可发送时刻（当日或次日 09:00）；否则返回当前时刻
     */
    public LocalDateTime resolveDeliveryTime(LocalDateTime dateTime) {
        if (dateTime == null || !isQuietHours(dateTime)) {
            return dateTime != null ? dateTime : LocalDateTime.now(resolveZoneId());
        }
        QuietHoursConfig config = loadConfig();
        LocalDate date = dateTime.toLocalDate();
        LocalTime quietEnd = config.getQuietEnd();
        LocalTime time = dateTime.toLocalTime();

        if (time.isBefore(quietEnd)) {
            return date.atTime(quietEnd);
        }
        return date.plusDays(1).atTime(quietEnd);
    }

    public Date resolveDeliveryDate(LocalDateTime dateTime) {
        LocalDateTime delivery = resolveDeliveryTime(dateTime);
        return Date.from(delivery.atZone(resolveZoneId()).toInstant());
    }

    public ZoneId resolveZoneId() {
        return ZoneId.of(loadConfig().getTimezone());
    }

    private QuietHoursConfig loadConfig() {
        List<SystemConfigPO> slaConfigs = systemConfigMapper.selectByGroup(GROUP_SLA);
        Map<String, String> slaMap = slaConfigs.stream()
                .collect(Collectors.toMap(SystemConfigPO::getConfigKey, SystemConfigPO::getConfigValue, (a, b) -> a));

        LambdaQueryWrapper<SystemConfigPO> tzWrapper = new LambdaQueryWrapper<>();
        tzWrapper.eq(SystemConfigPO::getConfigKey, KEY_TIMEZONE);
        SystemConfigPO tzConfig = systemConfigMapper.selectOne(tzWrapper);

        String startStr = slaMap.getOrDefault(KEY_QUIET_START, DEFAULT_QUIET_START);
        String endStr = slaMap.getOrDefault(KEY_QUIET_END, DEFAULT_QUIET_END);
        String timezone = tzConfig != null && tzConfig.getConfigValue() != null
                ? tzConfig.getConfigValue() : DEFAULT_TIMEZONE;

        try {
            return new QuietHoursConfig(LocalTime.parse(startStr), LocalTime.parse(endStr), timezone);
        } catch (Exception e) {
            log.warn("SLA静默时段配置解析失败，使用默认 22:00-09:00: start={}, end={}", startStr, endStr);
            return new QuietHoursConfig(LocalTime.parse(DEFAULT_QUIET_START), LocalTime.parse(DEFAULT_QUIET_END), DEFAULT_TIMEZONE);
        }
    }

    private static class QuietHoursConfig {
        private final LocalTime quietStart;
        private final LocalTime quietEnd;
        private final String timezone;

        QuietHoursConfig(LocalTime quietStart, LocalTime quietEnd, String timezone) {
            this.quietStart = quietStart;
            this.quietEnd = quietEnd;
            this.timezone = timezone;
        }

        LocalTime getQuietStart() {
            return quietStart;
        }

        LocalTime getQuietEnd() {
            return quietEnd;
        }

        String getTimezone() {
            return timezone;
        }
    }
}
