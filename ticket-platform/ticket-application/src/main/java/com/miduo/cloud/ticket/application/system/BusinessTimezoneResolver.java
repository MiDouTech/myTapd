package com.miduo.cloud.ticket.application.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.util.DisplayTimeFormat;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 业务时区解析器：SLA、工作时间等与页面展示保持同一时区口径。
 * 避免容器 JVM 默认 UTC 与 Jackson 展示时区（Asia/Shanghai）不一致导致 SLA 截止偏差 8 小时。
 */
@Component
public class BusinessTimezoneResolver {

    private static final Logger log = LoggerFactory.getLogger(BusinessTimezoneResolver.class);

    private static final String CONFIG_GROUP_BASIC = "BASIC";
    private static final String CONFIG_KEY_TIMEZONE = "timezone";

    private final SystemConfigMapper systemConfigMapper;

    private volatile String cachedTimezone;
    private volatile long cacheExpiresAtMs;
    private static final long CACHE_TTL_MS = 60_000L;

    public BusinessTimezoneResolver(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public ZoneId resolveZoneId() {
        return ZoneId.of(resolveTimezoneId());
    }

    public LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(resolveZoneId());
    }

    public LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return nowLocalDateTime();
        }
        return date.toInstant().atZone(resolveZoneId()).toLocalDateTime();
    }

    public Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(resolveZoneId()).toInstant());
    }

    private String resolveTimezoneId() {
        long now = System.currentTimeMillis();
        String cached = cachedTimezone;
        if (cached != null && now < cacheExpiresAtMs) {
            return cached;
        }
        synchronized (this) {
            if (cachedTimezone != null && System.currentTimeMillis() < cacheExpiresAtMs) {
                return cachedTimezone;
            }
            String resolved = resolveTimezoneFromDb();
            cachedTimezone = resolved;
            cacheExpiresAtMs = System.currentTimeMillis() + CACHE_TTL_MS;
            return resolved;
        }
    }

    private String resolveTimezoneFromDb() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, CONFIG_GROUP_BASIC)
                .eq(SystemConfigPO::getConfigKey, CONFIG_KEY_TIMEZONE)
                .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO timezoneConfig = systemConfigMapper.selectOne(wrapper);
        if (timezoneConfig == null || timezoneConfig.getConfigValue() == null
                || timezoneConfig.getConfigValue().trim().isEmpty()) {
            return DisplayTimeFormat.TIMEZONE_ID;
        }
        String timezone = timezoneConfig.getConfigValue().trim();
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (DateTimeException ex) {
            log.warn("读取到非法业务时区配置，timezone={}，回退默认时区={}", timezone, DisplayTimeFormat.TIMEZONE_ID);
            return DisplayTimeFormat.TIMEZONE_ID;
        }
    }
}
