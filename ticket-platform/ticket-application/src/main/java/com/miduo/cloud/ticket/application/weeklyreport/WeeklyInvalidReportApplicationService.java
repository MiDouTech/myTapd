package com.miduo.cloud.ticket.application.weeklyreport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.notification.sender.WecomGroupWebhookSender;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportConfigOutput;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportConfigUpdateInput;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportOutput;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportOutput.WeeklyInvalidReporterItem;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportOutput.WeeklyInvalidReportSummary;
import com.miduo.cloud.ticket.entity.dto.weeklyreport.WeeklyInvalidReportOutput.WeeklyInvalidTicketItem;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.mapper.DailyReportMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.WeeklyInvalidReporterRow;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.WeeklyInvalidTicketRow;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 无效反馈周报生成与推送应用服务
 */
@Service
public class WeeklyInvalidReportApplicationService extends BaseApplicationService {

    private static final String CONFIG_GROUP = "WEEKLY_INVALID_REPORT";
    private static final String BASIC_CONFIG_GROUP = "BASIC";
    private static final String BASIC_CONFIG_KEY_TIMEZONE = "timezone";

    private static final String CONFIG_KEY_ENABLED = "weekly_invalid_report_enabled";
    private static final String CONFIG_KEY_CRON = "weekly_invalid_report_cron";
    private static final String CONFIG_KEY_WEBHOOK_URLS = "weekly_invalid_report_webhook_urls";
    private static final String CONFIG_KEY_STAT_CATEGORY_IDS = "weekly_invalid_report_stat_category_ids";
    private static final String CONFIG_KEY_MAX_DETAIL_COUNT = "weekly_invalid_report_max_detail_count";
    private static final String CONFIG_KEY_TIMEZONE = "weekly_invalid_report_timezone";

    private static final String DEFAULT_CRON = "0 0 18 ? * FRI";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final int DEFAULT_MAX_DETAIL_COUNT = 30;
    private static final int MAX_DETAIL_COUNT = 200;
    private static final String CRON_SEPARATOR = ";";

    private static final long CONFIG_CACHE_TTL_MS = 60_000L;

    private final DailyReportMapper dailyReportMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final WecomGroupWebhookSender groupWebhookSender;

    private volatile Map<String, String> weeklyReportConfigCache;
    private volatile long weeklyReportConfigCacheExpiresAtMs;

    public WeeklyInvalidReportApplicationService(DailyReportMapper dailyReportMapper,
                                                 SystemConfigMapper systemConfigMapper,
                                                 WecomGroupWebhookSender groupWebhookSender) {
        this.dailyReportMapper = dailyReportMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.groupWebhookSender = groupWebhookSender;
    }

    /**
     * 生成无效反馈周报
     */
    public WeeklyInvalidReportOutput generateWeeklyInvalidReport() {
        Map<String, String> configMap = loadWeeklyReportConfigMap();
        String timezone = resolveScheduleTimezone(configMap.get(CONFIG_KEY_TIMEZONE));
        Date now = new Date();
        Date weekStart = startOfWeek(now, timezone);
        Date reportEnd = now;

        List<Long> statCategoryIds = parseStatCategoryIds(configMap.get(CONFIG_KEY_STAT_CATEGORY_IDS));
        int maxDetailCount = parseMaxDetailCount(configMap.get(CONFIG_KEY_MAX_DETAIL_COUNT));

        long invalidTotalCount = safeLong(
                dailyReportMapper.selectWeeklyInvalidFeedbackTotal(weekStart, reportEnd, statCategoryIds)
        );
        List<WeeklyInvalidReporterRow> reporterRows = dailyReportMapper.selectWeeklyInvalidFeedbackByReporter(
                weekStart, reportEnd, statCategoryIds
        );
        List<WeeklyInvalidTicketRow> detailRows = dailyReportMapper.selectWeeklyInvalidFeedbackDetail(
                weekStart, reportEnd, statCategoryIds, maxDetailCount
        );

        WeeklyInvalidReportOutput output = new WeeklyInvalidReportOutput();
        output.setReportDate(formatDate(now, "yyyy-MM-dd", timezone));
        output.setWeekRangeLabel(formatDate(weekStart, "yyyy-MM-dd", timezone)
                + " ~ "
                + formatDate(reportEnd, "yyyy-MM-dd", timezone));

        WeeklyInvalidReportSummary summary = new WeeklyInvalidReportSummary();
        summary.setInvalidTotalCount(invalidTotalCount);
        summary.setReporterCount(reporterRows == null ? 0 : reporterRows.size());
        summary.setDetailDisplayCount(detailRows == null ? 0 : detailRows.size());
        summary.setDetailLimitCount(maxDetailCount);
        output.setSummary(summary);

        output.setReporterStats(toReporterItems(reporterRows));
        output.setTicketDetails(toTicketItems(detailRows));
        output.setMarkdownContent(renderMarkdown(output, timezone));
        return output;
    }

    /**
     * 自动推送无效反馈周报
     */
    public void pushWeeklyInvalidReport() {
        pushWeeklyInvalidReportInternal(false);
    }

    /**
     * 手动推送无效反馈周报（忽略自动开关）
     */
    public void pushWeeklyInvalidReportManually() {
        pushWeeklyInvalidReportInternal(true);
    }

    /**
     * 查询周报配置
     */
    public WeeklyInvalidReportConfigOutput getConfig() {
        Map<String, String> configMap = loadWeeklyReportConfigMap();
        WeeklyInvalidReportConfigOutput output = new WeeklyInvalidReportConfigOutput();
        output.setEnabled("true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_ENABLED, "false")));
        output.setCronList(parseCronList(configMap.getOrDefault(CONFIG_KEY_CRON, DEFAULT_CRON)));
        output.setWebhookUrls(parseWebhookUrls(configMap.get(CONFIG_KEY_WEBHOOK_URLS)));
        output.setStatCategoryIds(parseStatCategoryIds(configMap.get(CONFIG_KEY_STAT_CATEGORY_IDS)));
        output.setMaxDetailCount(parseMaxDetailCount(configMap.get(CONFIG_KEY_MAX_DETAIL_COUNT)));
        output.setTimezone(resolveScheduleTimezone(configMap.get(CONFIG_KEY_TIMEZONE)));
        return output;
    }

    /**
     * 更新周报配置
     */
    public void updateConfig(WeeklyInvalidReportConfigUpdateInput input) {
        if (input.getEnabled() != null) {
            upsertConfig(CONFIG_KEY_ENABLED, String.valueOf(input.getEnabled()));
        }
        if (input.getCronList() != null) {
            String joinedCron = input.getCronList().stream()
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.joining(CRON_SEPARATOR));
            if (!joinedCron.isEmpty()) {
                upsertConfig(CONFIG_KEY_CRON, joinedCron);
            }
        }
        if (input.getWebhookUrls() != null) {
            String joinedWebhook = input.getWebhookUrls().stream()
                    .filter(w -> w != null && !w.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.joining(","));
            upsertConfig(CONFIG_KEY_WEBHOOK_URLS, joinedWebhook);
        }
        if (input.getStatCategoryIds() != null) {
            upsertConfig(CONFIG_KEY_STAT_CATEGORY_IDS, joinStatCategoryIds(input.getStatCategoryIds()));
        }
        if (input.getMaxDetailCount() != null) {
            int safeCount = Math.max(1, Math.min(input.getMaxDetailCount(), MAX_DETAIL_COUNT));
            upsertConfig(CONFIG_KEY_MAX_DETAIL_COUNT, String.valueOf(safeCount));
        }
        if (input.getTimezone() != null && !input.getTimezone().trim().isEmpty()) {
            String timezone = input.getTimezone().trim();
            try {
                ZoneId.of(timezone);
                upsertConfig(CONFIG_KEY_TIMEZONE, timezone);
            } catch (DateTimeException ex) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "时区配置无效，请填写标准时区ID，例如 Asia/Shanghai");
            }
        }
        invalidateConfigCache();
    }

    /**
     * 获取推送 cron 列表
     */
    public List<String> getPushCronList() {
        Map<String, String> configMap = loadWeeklyReportConfigMap();
        return parseCronList(configMap.getOrDefault(CONFIG_KEY_CRON, DEFAULT_CRON));
    }

    /**
     * 自动推送是否启用
     */
    public boolean isEnabled() {
        Map<String, String> configMap = loadWeeklyReportConfigMap();
        return "true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_ENABLED, "false"));
    }

    /**
     * 获取调度时区
     */
    public String getScheduleTimezone() {
        Map<String, String> configMap = loadWeeklyReportConfigMap();
        return resolveScheduleTimezone(configMap.get(CONFIG_KEY_TIMEZONE));
    }

    private void pushWeeklyInvalidReportInternal(boolean manualTrigger) {
        Map<String, String> configMap = loadWeeklyReportConfigMap();
        boolean enabled = "true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_ENABLED, "false"));
        if (!enabled) {
            if (manualTrigger) {
                log.info("无效反馈周报自动推送开关关闭，但本次为手动推送，继续执行");
            } else {
                log.info("无效反馈周报自动推送已关闭，跳过推送");
                return;
            }
        }

        String webhookUrls = configMap.getOrDefault(CONFIG_KEY_WEBHOOK_URLS, "");
        if (webhookUrls.trim().isEmpty()) {
            if (manualTrigger) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "请先在无效反馈周报配置中填写企微群Webhook地址");
            }
            log.warn("无效反馈周报未配置Webhook地址，跳过推送");
            return;
        }

        WeeklyInvalidReportOutput report = generateWeeklyInvalidReport();
        String markdown = report.getMarkdownContent();
        if (markdown == null || markdown.trim().isEmpty()) {
            if (manualTrigger) {
                throw BusinessException.of(ErrorCode.DATA_STATUS_ERROR, "无效反馈周报内容为空，无法推送");
            }
            log.warn("无效反馈周报内容为空，跳过推送");
            return;
        }

        int validWebhookCount = 0;
        int successCount = 0;
        List<String> failedReasons = new ArrayList<>();
        for (String webhookUrl : webhookUrls.split(",")) {
            String trimmedUrl = webhookUrl.trim();
            if (trimmedUrl.isEmpty()) {
                continue;
            }
            validWebhookCount++;
            try {
                groupWebhookSender.sendReportNoticeToWebhook(trimmedUrl, markdown, null);
                successCount++;
            } catch (Exception ex) {
                failedReasons.add(sanitizePushErrorReason(ex.getMessage()));
                log.error("无效反馈周报推送失败: webhook={}, reason={}",
                        sanitizeWebhookUrl(trimmedUrl), ex.getMessage(), ex);
            }
        }

        if (validWebhookCount == 0) {
            if (manualTrigger) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "无效反馈周报配置中的Webhook地址无效，请检查后重试");
            }
            log.warn("无效反馈周报Webhook地址全为空，跳过推送");
            return;
        }

        if (successCount == 0) {
            String reason = failedReasons.isEmpty()
                    ? "未获取到可用Webhook地址"
                    : String.join("；", new LinkedHashSet<>(failedReasons));
            if (manualTrigger) {
                throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "无效反馈周报推送失败：" + reason);
            }
            log.error("无效反馈周报自动推送失败：全部Webhook发送失败，details={}", reason);
            return;
        }

        if (!failedReasons.isEmpty()) {
            log.warn("无效反馈周报推送部分失败：successCount={}, failedReasons={}",
                    successCount, new LinkedHashSet<>(failedReasons));
        }
    }

    private String renderMarkdown(WeeklyInvalidReportOutput report, String timezone) {
        StringBuilder markdown = new StringBuilder();
        WeeklyInvalidReportSummary summary = report.getSummary();
        boolean truncated = summary.getInvalidTotalCount() > summary.getDetailDisplayCount();

        markdown.append("**").append(report.getReportDate()).append(" 无效反馈周报**\n");
        markdown.append("统计区间：").append(report.getWeekRangeLabel()).append("\n");
        markdown.append("本周无效反馈总数：").append(summary.getInvalidTotalCount()).append("个");
        markdown.append("，涉及反馈人：").append(summary.getReporterCount()).append("人\n");

        markdown.append("\n**1、按反馈人统计**\n");
        List<WeeklyInvalidReporterItem> reporterStats = report.getReporterStats();
        if (reporterStats == null || reporterStats.isEmpty()) {
            markdown.append("  无\n");
        } else {
            int index = 1;
            for (WeeklyInvalidReporterItem item : reporterStats) {
                markdown.append(index)
                        .append(". ")
                        .append(safeName(item.getReporterName()))
                        .append("：")
                        .append(item.getInvalidCount())
                        .append("个\n");
                index++;
            }
        }

        markdown.append("\n**2、无效反馈明细**\n");
        List<WeeklyInvalidTicketItem> ticketDetails = report.getTicketDetails();
        if (ticketDetails == null || ticketDetails.isEmpty()) {
            markdown.append("  无\n");
        } else {
            int index = 1;
            for (WeeklyInvalidTicketItem item : ticketDetails) {
                markdown.append(index)
                        .append(". [")
                        .append(item.getTicketNo() == null ? "-" : item.getTicketNo())
                        .append("] ")
                        .append(item.getTitle() == null ? "无标题" : item.getTitle())
                        .append(" —— 反馈人：")
                        .append(safeName(item.getReporterName()));
                if (item.getClosedTime() != null) {
                    markdown.append("，关闭时间：")
                            .append(formatDate(item.getClosedTime(), "yyyy-MM-dd HH:mm", timezone));
                }
                markdown.append("\n");
                index++;
            }
        }

        if (truncated) {
            markdown.append("\n> 当前仅展示前")
                    .append(summary.getDetailLimitCount())
                    .append("条明细，请在系统内查看完整列表。\n");
        }
        return markdown.toString();
    }

    private List<WeeklyInvalidReporterItem> toReporterItems(List<WeeklyInvalidReporterRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<WeeklyInvalidReporterItem> items = new ArrayList<>(rows.size());
        for (WeeklyInvalidReporterRow row : rows) {
            WeeklyInvalidReporterItem item = new WeeklyInvalidReporterItem();
            item.setReporterId(row.getReporterId());
            item.setReporterName(safeName(row.getReporterName()));
            item.setInvalidCount(safeLong(row.getTotal()));
            items.add(item);
        }
        return items;
    }

    private List<WeeklyInvalidTicketItem> toTicketItems(List<WeeklyInvalidTicketRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<WeeklyInvalidTicketItem> items = new ArrayList<>(rows.size());
        for (WeeklyInvalidTicketRow row : rows) {
            WeeklyInvalidTicketItem item = new WeeklyInvalidTicketItem();
            item.setId(row.getId());
            item.setTicketNo(row.getTicketNo());
            item.setTitle(row.getTitle());
            item.setReporterId(row.getReporterId());
            item.setReporterName(safeName(row.getReporterName()));
            item.setClosedTime(row.getClosedTime());
            items.add(item);
        }
        return items;
    }

    private Date startOfWeek(Date date, String timezone) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int offset = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
        calendar.add(Calendar.DAY_OF_MONTH, offset);
        return calendar.getTime();
    }

    private String formatDate(Date date, String pattern, String timezone) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        return formatter.format(date);
    }

    private Map<String, String> loadWeeklyReportConfigMap() {
        long now = System.currentTimeMillis();
        Map<String, String> cached = weeklyReportConfigCache;
        if (cached != null && now < weeklyReportConfigCacheExpiresAtMs) {
            return cached;
        }
        synchronized (this) {
            if (weeklyReportConfigCache != null && System.currentTimeMillis() < weeklyReportConfigCacheExpiresAtMs) {
                return weeklyReportConfigCache;
            }
            Map<String, String> fresh = fetchWeeklyReportConfigMapFromDb();
            weeklyReportConfigCache = fresh;
            weeklyReportConfigCacheExpiresAtMs = System.currentTimeMillis() + CONFIG_CACHE_TTL_MS;
            return fresh;
        }
    }

    private Map<String, String> fetchWeeklyReportConfigMapFromDb() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, CONFIG_GROUP)
                .eq(SystemConfigPO::getDeleted, 0);
        List<SystemConfigPO> configs = systemConfigMapper.selectList(wrapper);
        Map<String, String> configMap = new HashMap<>();
        if (configs != null) {
            for (SystemConfigPO config : configs) {
                if (config.getConfigKey() != null && config.getConfigValue() != null) {
                    configMap.put(config.getConfigKey(), config.getConfigValue());
                }
            }
        }
        return configMap;
    }

    private void invalidateConfigCache() {
        weeklyReportConfigCache = null;
        weeklyReportConfigCacheExpiresAtMs = 0L;
    }

    private void upsertConfig(String configKey, String configValue) {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, CONFIG_GROUP)
                .eq(SystemConfigPO::getConfigKey, configKey)
                .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO existing = systemConfigMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setConfigValue(configValue);
            existing.setUpdateBy("system");
            existing.setUpdateTime(new Date());
            systemConfigMapper.updateById(existing);
        } else {
            SystemConfigPO config = new SystemConfigPO();
            config.setConfigGroup(CONFIG_GROUP);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setDescription("无效反馈周报配置-" + configKey);
            config.setCreateBy("system");
            config.setUpdateBy("system");
            systemConfigMapper.insert(config);
        }
    }

    private List<String> parseCronList(String cronValue) {
        if (cronValue == null || cronValue.trim().isEmpty()) {
            return Collections.singletonList(DEFAULT_CRON);
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String cron : cronValue.split(CRON_SEPARATOR)) {
            if (cron != null && !cron.trim().isEmpty()) {
                unique.add(cron.trim());
            }
        }
        return unique.isEmpty() ? Collections.singletonList(DEFAULT_CRON) : new ArrayList<>(unique);
    }

    private List<String> parseWebhookUrls(String webhookValue) {
        if (webhookValue == null || webhookValue.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> urls = new ArrayList<>();
        for (String item : webhookValue.split(",")) {
            if (item != null && !item.trim().isEmpty()) {
                urls.add(item.trim());
            }
        }
        return urls;
    }

    private List<Long> parseStatCategoryIds(String categoryIdValue) {
        if (categoryIdValue == null || categoryIdValue.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> categoryIdSet = new LinkedHashSet<>();
        for (String item : categoryIdValue.split(",")) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }
            try {
                categoryIdSet.add(Long.parseLong(item.trim()));
            } catch (NumberFormatException ex) {
                log.warn("无效反馈周报统计分类ID配置包含非法值，value={}", item);
            }
        }
        return new ArrayList<>(categoryIdSet);
    }

    private String joinStatCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return "";
        }
        List<String> items = new ArrayList<>();
        Set<Long> dedup = new HashSet<>();
        for (Long categoryId : categoryIds) {
            if (categoryId != null && dedup.add(categoryId)) {
                items.add(String.valueOf(categoryId));
            }
        }
        return String.join(",", items);
    }

    private int parseMaxDetailCount(String maxDetailCountValue) {
        if (maxDetailCountValue == null || maxDetailCountValue.trim().isEmpty()) {
            return DEFAULT_MAX_DETAIL_COUNT;
        }
        try {
            int parsed = Integer.parseInt(maxDetailCountValue.trim());
            if (parsed <= 0) {
                return DEFAULT_MAX_DETAIL_COUNT;
            }
            return Math.min(parsed, MAX_DETAIL_COUNT);
        } catch (NumberFormatException ex) {
            log.warn("无效反馈周报明细条数配置非法，value={}", maxDetailCountValue);
            return DEFAULT_MAX_DETAIL_COUNT;
        }
    }

    private String resolveScheduleTimezone(String timezoneValue) {
        if (timezoneValue != null && !timezoneValue.trim().isEmpty()) {
            return normalizeTimezone(timezoneValue.trim(), DEFAULT_TIMEZONE);
        }
        String basicTimezone = readBasicTimezoneConfig();
        if (basicTimezone != null && !basicTimezone.trim().isEmpty()) {
            return normalizeTimezone(basicTimezone.trim(), DEFAULT_TIMEZONE);
        }
        return DEFAULT_TIMEZONE;
    }

    private String readBasicTimezoneConfig() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, BASIC_CONFIG_GROUP)
                .eq(SystemConfigPO::getConfigKey, BASIC_CONFIG_KEY_TIMEZONE)
                .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO timezoneConfig = systemConfigMapper.selectOne(wrapper);
        return timezoneConfig == null ? null : timezoneConfig.getConfigValue();
    }

    private String normalizeTimezone(String timezone, String defaultTimezone) {
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (DateTimeException ex) {
            log.warn("无效反馈周报读取到非法时区配置，timezone={}，回退默认时区={}", timezone, defaultTimezone);
            return defaultTimezone;
        }
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeName(String value) {
        return value == null || value.trim().isEmpty() ? "未知" : value.trim();
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

    private String sanitizePushErrorReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "未知错误";
        }
        String normalized = reason.trim().replaceAll("\\s+", " ");
        String lowerCase = normalized.toLowerCase(Locale.ROOT);
        if ((lowerCase.contains("exceed max length") && lowerCase.contains("4096"))
                || lowerCase.contains("markdown.content exceed max length 4096")) {
            return "周报内容超过企微单条消息上限（4096字符），请减少明细条数后重试";
        }
        if (lowerCase.contains("timed out") || lowerCase.contains("timeout")) {
            return "连接企微超时，请稍后重试";
        }
        if (lowerCase.contains("connection refused") || lowerCase.contains("connectexception")) {
            return "无法连接企微服务，请检查网络或Webhook可用性";
        }
        if (lowerCase.contains("invalid webhook") || lowerCase.contains("webhook key")) {
            return "Webhook地址无效或已失效，请在周报配置中更新后重试";
        }
        if (normalized.startsWith("发送企微群Webhook失败:")) {
            normalized = normalized.substring("发送企微群Webhook失败:".length()).trim();
        }
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }
}
