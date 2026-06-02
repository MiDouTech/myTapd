package com.miduo.cloud.ticket.application.weeklyreport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.dailyreport.DailyReportApplicationService;
import com.miduo.cloud.ticket.application.notification.sender.WecomGroupWebhookSender;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportConfigOutput;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportConfigUpdateInput;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * 无效反馈月报（并入日报管理）生成与推送应用服务
 */
@Service
public class WeeklyInvalidReportApplicationService extends BaseApplicationService {

    private static final String LEGACY_CONFIG_GROUP = "WEEKLY_INVALID_REPORT";
    private static final String LEGACY_CONFIG_KEY_MAX_DETAIL_COUNT = "weekly_invalid_report_max_detail_count";
    private static final String BASIC_CONFIG_GROUP = "BASIC";
    private static final String BASIC_CONFIG_KEY_TIMEZONE = "timezone";

    private static final int DEFAULT_MAX_DETAIL_COUNT = 30;
    private static final int MAX_DETAIL_COUNT = 200;
    private final DailyReportApplicationService dailyReportService;
    private final DailyReportMapper dailyReportMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final WecomGroupWebhookSender groupWebhookSender;

    public WeeklyInvalidReportApplicationService(DailyReportApplicationService dailyReportService,
                                                 DailyReportMapper dailyReportMapper,
                                                 SystemConfigMapper systemConfigMapper,
                                                 WecomGroupWebhookSender groupWebhookSender) {
        this.dailyReportService = dailyReportService;
        this.dailyReportMapper = dailyReportMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.groupWebhookSender = groupWebhookSender;
    }

    /**
     * 生成无效反馈月报（统计范围与日报管理一致：自然月）
     */
    public WeeklyInvalidReportOutput generateWeeklyInvalidReport() {
        DailyReportConfigOutput dailyConfig = dailyReportService.getConfig();
        String timezone = dailyReportService.getScheduleTimezone();
        Date now = new Date();
        Date monthStart = startOfMonth(now, timezone);
        Date monthEnd = addMonths(monthStart, 1, timezone);
        Date monthDisplayEnd = addMilliseconds(monthEnd, -1, timezone);

        List<Long> statCategoryIds = safeStatCategoryIds(dailyConfig.getStatCategoryIds());
        int maxDetailCount = loadMaxDetailCount();

        long invalidTotalCount = safeLong(
                dailyReportMapper.selectWeeklyInvalidFeedbackTotal(monthStart, monthEnd, statCategoryIds)
        );
        List<WeeklyInvalidReporterRow> reporterRows = dailyReportMapper.selectWeeklyInvalidFeedbackByReporter(
                monthStart, monthEnd, statCategoryIds
        );
        List<WeeklyInvalidTicketRow> detailRows = dailyReportMapper.selectWeeklyInvalidFeedbackDetail(
                monthStart, monthEnd, statCategoryIds, maxDetailCount
        );

        WeeklyInvalidReportOutput output = new WeeklyInvalidReportOutput();
        output.setReportDate(formatDate(now, "yyyy-MM-dd", timezone));
        output.setWeekRangeLabel(formatDate(monthStart, "yyyy-MM-dd", timezone)
                + " ~ "
                + formatDate(monthDisplayEnd, "yyyy-MM-dd", timezone));

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
     * 自动推送无效反馈月报
     */
    public void pushWeeklyInvalidReport() {
        pushWeeklyInvalidReportInternal(false);
    }

    /**
     * 手动推送无效反馈月报（忽略自动开关）
     */
    public void pushWeeklyInvalidReportManually() {
        pushWeeklyInvalidReportInternal(true);
    }

    /**
     * 兼容接口：查询无效反馈报表配置（实际复用日报配置）
     */
    public WeeklyInvalidReportConfigOutput getConfig() {
        DailyReportConfigOutput dailyConfig = dailyReportService.getConfig();
        WeeklyInvalidReportConfigOutput output = new WeeklyInvalidReportConfigOutput();
        output.setEnabled(dailyConfig.isEnabled());
        output.setCronList(copyStringList(dailyConfig.getCronList()));
        output.setWebhookUrls(copyStringList(dailyConfig.getWebhookUrls()));
        output.setStatCategoryIds(copyLongList(dailyConfig.getStatCategoryIds()));
        output.setMaxDetailCount(loadMaxDetailCount());
        output.setTimezone(dailyReportService.getScheduleTimezone());
        return output;
    }

    /**
     * 兼容接口：更新无效反馈报表配置（实际更新日报配置）
     */
    public void updateConfig(WeeklyInvalidReportConfigUpdateInput input) {
        DailyReportConfigUpdateInput dailyInput = new DailyReportConfigUpdateInput();
        boolean updateDailyConfig = false;
        if (input.getEnabled() != null) {
            dailyInput.setEnabled(input.getEnabled());
            updateDailyConfig = true;
        }
        if (input.getCronList() != null) {
            dailyInput.setCronList(input.getCronList());
            updateDailyConfig = true;
        }
        if (input.getWebhookUrls() != null) {
            dailyInput.setWebhookUrls(input.getWebhookUrls());
            updateDailyConfig = true;
        }
        if (input.getStatCategoryIds() != null) {
            dailyInput.setStatCategoryIds(input.getStatCategoryIds());
            updateDailyConfig = true;
        }
        if (updateDailyConfig) {
            dailyReportService.updateConfig(dailyInput);
        }
        if (input.getMaxDetailCount() != null) {
            int safeCount = Math.max(1, Math.min(input.getMaxDetailCount(), MAX_DETAIL_COUNT));
            upsertConfig(LEGACY_CONFIG_GROUP,
                    LEGACY_CONFIG_KEY_MAX_DETAIL_COUNT,
                    String.valueOf(safeCount),
                    "无效反馈月报配置-" + LEGACY_CONFIG_KEY_MAX_DETAIL_COUNT);
        }
        if (input.getTimezone() != null && !input.getTimezone().trim().isEmpty()) {
            String timezone = input.getTimezone().trim();
            try {
                ZoneId.of(timezone);
            } catch (DateTimeException ex) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "时区配置无效，请填写标准时区ID，例如 Asia/Shanghai");
            }
            // 无效反馈月报与日报必须共享时区，因此兼容接口写入 BASIC 时区配置。
            upsertConfig(BASIC_CONFIG_GROUP, BASIC_CONFIG_KEY_TIMEZONE, timezone, "系统时区配置");
        }
    }

    /**
     * 兼容保留：获取推送 cron 列表（与日报一致）
     */
    public List<String> getPushCronList() {
        return dailyReportService.getPushCronList();
    }

    /**
     * 兼容保留：自动推送是否启用（与日报一致）
     */
    public boolean isEnabled() {
        return dailyReportService.isEnabled();
    }

    /**
     * 兼容保留：调度时区（与日报一致）
     */
    public String getScheduleTimezone() {
        return dailyReportService.getScheduleTimezone();
    }

    private void pushWeeklyInvalidReportInternal(boolean manualTrigger) {
        DailyReportConfigOutput dailyConfig = dailyReportService.getConfig();
        boolean enabled = dailyConfig.isEnabled();
        if (!enabled) {
            if (manualTrigger) {
                log.info("日报自动推送开关关闭，但本次为手动推送无效反馈月报，继续执行");
            } else {
                log.info("日报自动推送已关闭，跳过无效反馈月报推送");
                return;
            }
        }

        List<String> webhookUrls = copyStringList(dailyConfig.getWebhookUrls());
        if (webhookUrls.isEmpty()) {
            if (manualTrigger) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "请先在日报配置中填写企微群Webhook地址");
            }
            log.warn("日报配置未填写Webhook地址，跳过无效反馈月报推送");
            return;
        }

        WeeklyInvalidReportOutput report = generateWeeklyInvalidReport();
        String markdown = report.getMarkdownContent();
        if (markdown == null || markdown.trim().isEmpty()) {
            if (manualTrigger) {
                throw BusinessException.of(ErrorCode.DATA_STATUS_ERROR, "无效反馈月报内容为空，无法推送");
            }
            log.warn("无效反馈月报内容为空，跳过推送");
            return;
        }

        int validWebhookCount = 0;
        int successCount = 0;
        List<String> failedReasons = new ArrayList<>();
        for (String webhookUrl : webhookUrls) {
            String trimmedUrl = webhookUrl == null ? "" : webhookUrl.trim();
            if (trimmedUrl.isEmpty()) {
                continue;
            }
            validWebhookCount++;
            try {
                groupWebhookSender.sendReportNoticeToWebhook(trimmedUrl, markdown, null);
                successCount++;
            } catch (Exception ex) {
                failedReasons.add(sanitizePushErrorReason(ex.getMessage()));
                log.error("无效反馈月报推送失败: webhook={}, reason={}",
                        sanitizeWebhookUrl(trimmedUrl), ex.getMessage(), ex);
            }
        }

        if (validWebhookCount == 0) {
            if (manualTrigger) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID, "日报配置中的Webhook地址无效，请检查后重试");
            }
            log.warn("日报配置中的Webhook地址全为空，跳过无效反馈月报推送");
            return;
        }

        if (successCount == 0) {
            String reason = failedReasons.isEmpty()
                    ? "未获取到可用Webhook地址"
                    : String.join("；", new LinkedHashSet<>(failedReasons));
            if (manualTrigger) {
                throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "无效反馈月报推送失败：" + reason);
            }
            log.error("无效反馈月报自动推送失败：全部Webhook发送失败，details={}", reason);
            return;
        }

        if (!failedReasons.isEmpty()) {
            log.warn("无效反馈月报推送部分失败：successCount={}, failedReasons={}",
                    successCount, new LinkedHashSet<>(failedReasons));
        }
    }

    private String renderMarkdown(WeeklyInvalidReportOutput report, String timezone) {
        StringBuilder markdown = new StringBuilder();
        WeeklyInvalidReportSummary summary = report.getSummary();
        boolean truncated = summary.getInvalidTotalCount() > summary.getDetailDisplayCount();

        markdown.append("**").append(report.getReportDate()).append(" 无效反馈月报**\n");
        markdown.append("统计区间：").append(report.getWeekRangeLabel()).append("\n");
        markdown.append("本月无效反馈总数：").append(summary.getInvalidTotalCount()).append("个");
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

    private Date startOfMonth(Date date, String timezone) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date addMonths(Date date, int monthOffset, String timezone) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, monthOffset);
        return calendar.getTime();
    }

    private Date addMilliseconds(Date date, int millisecondOffset, String timezone) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        calendar.setTime(date);
        calendar.add(Calendar.MILLISECOND, millisecondOffset);
        return calendar.getTime();
    }

    private String formatDate(Date date, String pattern, String timezone) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        formatter.setTimeZone(TimeZone.getTimeZone(timezone));
        return formatter.format(date);
    }

    private int loadMaxDetailCount() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, LEGACY_CONFIG_GROUP)
                .eq(SystemConfigPO::getConfigKey, LEGACY_CONFIG_KEY_MAX_DETAIL_COUNT)
                .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO config = systemConfigMapper.selectOne(wrapper);
        return parseMaxDetailCount(config == null ? null : config.getConfigValue());
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
            log.warn("无效反馈月报明细条数配置非法，value={}", maxDetailCountValue);
            return DEFAULT_MAX_DETAIL_COUNT;
        }
    }

    private void upsertConfig(String configGroup, String configKey, String configValue, String description) {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, configGroup)
                .eq(SystemConfigPO::getConfigKey, configKey)
                .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO existing = systemConfigMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setConfigValue(configValue);
            existing.setUpdateBy("system");
            existing.setUpdateTime(new Date());
            systemConfigMapper.updateById(existing);
            return;
        }
        SystemConfigPO config = new SystemConfigPO();
        config.setConfigGroup(configGroup);
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        config.setDescription(description);
        config.setCreateBy("system");
        config.setUpdateBy("system");
        systemConfigMapper.insert(config);
    }

    private List<Long> safeStatCategoryIds(List<Long> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long id : source) {
            if (id != null) {
                unique.add(id);
            }
        }
        return new ArrayList<>(unique);
    }

    private List<String> copyStringList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (String item : source) {
            if (item != null && !item.trim().isEmpty()) {
                values.add(item.trim());
            }
        }
        return values;
    }

    private List<Long> copyLongList(List<Long> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> values = new ArrayList<>();
        for (Long item : source) {
            if (item != null) {
                values.add(item);
            }
        }
        return values;
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
            return "月报内容超过企微单条消息上限（4096字符），请减少明细条数后重试";
        }
        if (lowerCase.contains("timed out") || lowerCase.contains("timeout")) {
            return "连接企微超时，请稍后重试";
        }
        if (lowerCase.contains("connection refused") || lowerCase.contains("connectexception")) {
            return "无法连接企微服务，请检查网络或Webhook可用性";
        }
        if (lowerCase.contains("invalid webhook") || lowerCase.contains("webhook key")) {
            return "Webhook地址无效或已失效，请在日报配置中更新后重试";
        }
        if (normalized.startsWith("发送企微群Webhook失败:")) {
            normalized = normalized.substring("发送企微群Webhook失败:".length()).trim();
        }
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }
}
