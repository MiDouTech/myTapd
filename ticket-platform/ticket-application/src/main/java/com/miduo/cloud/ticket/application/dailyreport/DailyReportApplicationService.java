package com.miduo.cloud.ticket.application.dailyreport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.notification.sender.WecomGroupWebhookSender;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.util.DisplayTimeFormat;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportConfigOutput;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportConfigUpdateInput;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportOutput;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportOutput.DailyReportSection;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportOutput.DailyReportSubSection;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportOutput.DailyReportSummary;
import com.miduo.cloud.ticket.entity.dto.dailyreport.DailyReportOutput.DailyReportTicketItem;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.mapper.DailyReportMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.DailyReportStatusRow;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.DailyReportTicketRow;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日报生成与推送应用服务
 */
@Service
public class DailyReportApplicationService extends BaseApplicationService {

    private static final String CONFIG_KEY_ENABLED = "daily_report_enabled";
    private static final String CONFIG_KEY_CRON = "daily_report_cron";
    private static final String CONFIG_KEY_WEBHOOK_URLS = "daily_report_webhook_urls";
    private static final String CONFIG_KEY_INCLUDE_DEFECT_DETAIL = "daily_report_include_defect_detail";
    private static final String CONFIG_KEY_INCLUDE_SUSPENDED = "daily_report_include_suspended";
    private static final String CONFIG_GROUP = "DAILY_REPORT";
    private static final String BASIC_CONFIG_GROUP = "BASIC";
    private static final String BASIC_CONFIG_KEY_TIMEZONE = "timezone";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    private static final String CRON_SEPARATOR = ";";
    /**
     * 日报调度每 30 秒轮询配置；缓存可避免对 system_config 的重复全量扫描，减轻库压与连接占用（降低网关 502 风险）。
     */
    private static final long DAILY_REPORT_CONFIG_CACHE_TTL_MS = 60_000L;

    private final DailyReportMapper dailyReportMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final WecomGroupWebhookSender groupWebhookSender;

    private volatile Map<String, String> dailyReportConfigCache;
    private volatile long dailyReportConfigCacheExpiresAtMs;

    private volatile String scheduleTimezoneCache;
    private volatile long scheduleTimezoneCacheExpiresAtMs;

    public DailyReportApplicationService(DailyReportMapper dailyReportMapper,
                                         SystemConfigMapper systemConfigMapper,
                                         WecomGroupWebhookSender groupWebhookSender) {
        this.dailyReportMapper = dailyReportMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.groupWebhookSender = groupWebhookSender;
    }

    /**
     * 生成日报数据
     */
    public DailyReportOutput generateDailyReport() {
        Date now = new Date();
        Date startOfDay = startOfDay(now);
        Date endOfDay = addDays(startOfDay, 1);
        SimpleDateFormat sdf = DisplayTimeFormat.newFormatter("yyyy-MM-dd");
        String reportDate = sdf.format(now);

        Map<String, String> configMap = loadDailyReportConfigMap();
        boolean includeDefectDetail = "true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_INCLUDE_DEFECT_DETAIL, "true"));
        boolean includeSuspended = "true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_INCLUDE_SUSPENDED, "true"));

        Long totalFeedbackCount = safeLong(dailyReportMapper.selectTotalFeedbackCount());
        Long newIssueCountToday = safeLong(dailyReportMapper.selectCreatedCountByDateRange(startOfDay, endOfDay));

        List<DailyReportTicketRow> testingReproduceTickets = dailyReportMapper.selectTicketsByStatus("testing");
        List<DailyReportTicketRow> processingTickets = dailyReportMapper.selectProcessingTickets();
        List<DailyReportTicketRow> pendingVerifyTickets = dailyReportMapper.selectPendingVerifyTickets();
        List<DailyReportTicketRow> tempResolvedTickets = dailyReportMapper.selectTempResolvedTickets();
        List<DailyReportTicketRow> suspendedTickets = dailyReportMapper.selectSuspendedTickets();

        Long resolvedTodayCount = safeLong(dailyReportMapper.selectResolvedCountByDateRange(startOfDay, endOfDay));
        List<DailyReportStatusRow> closedByDefectType = dailyReportMapper.selectClosedByDefectType(startOfDay, endOfDay);
        DailyReportSection pendingSection = buildPendingSection(testingReproduceTickets, processingTickets, pendingVerifyTickets);
        long pendingResolveCount = pendingSection.getCount();

        DailyReportOutput output = new DailyReportOutput();
        output.setReportDate(reportDate);

        DailyReportSummary summary = new DailyReportSummary();
        summary.setTotalFeedbackCount(totalFeedbackCount);
        summary.setNewIssueCountToday(newIssueCountToday);
        summary.setPendingResolveCount(pendingResolveCount);
        summary.setTempResolvedCount(tempResolvedTickets.size());
        summary.setResolvedCount(resolvedTodayCount);
        summary.setSuspendedCount(suspendedTickets.size());
        output.setSummary(summary);

        output.setPendingSection(pendingSection);
        output.setTempResolvedSection(buildTempResolvedSection(tempResolvedTickets));
        output.setResolvedSection(buildResolvedSection(closedByDefectType, includeDefectDetail));

        if (includeSuspended) {
            output.setSuspendedSection(buildSuspendedSection(suspendedTickets));
        }

        output.setMarkdownContent(renderMarkdown(output, reportDate));

        return output;
    }

    /**
     * 推送日报到企微群
     */
    public void pushDailyReport() {
        Map<String, String> configMap = loadDailyReportConfigMap();
        boolean enabled = "true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_ENABLED, "false"));
        if (!enabled) {
            log.info("日报自动推送已关闭，跳过推送");
            return;
        }

        String webhookUrlsStr = configMap.getOrDefault(CONFIG_KEY_WEBHOOK_URLS, "");
        if (webhookUrlsStr.trim().isEmpty()) {
            log.warn("日报推送Webhook地址未配置，跳过推送");
            return;
        }

        DailyReportOutput report = generateDailyReport();
        String markdown = report.getMarkdownContent();
        if (markdown == null || markdown.trim().isEmpty()) {
            log.warn("日报内容为空，跳过推送");
            return;
        }

        String[] urls = webhookUrlsStr.split(",");
        for (String url : urls) {
            String trimmedUrl = url.trim();
            if (trimmedUrl.isEmpty()) {
                continue;
            }
            try {
                groupWebhookSender.sendReportNoticeToWebhook(trimmedUrl, markdown, null);
                log.info("日报推送成功: webhook={}", sanitizeWebhookUrl(trimmedUrl));
            } catch (Exception ex) {
                log.error("日报推送失败: webhook={}, reason={}", sanitizeWebhookUrl(trimmedUrl), ex.getMessage(), ex);
            }
        }
    }

    /**
     * 查询日报配置
     */
    public DailyReportConfigOutput getConfig() {
        Map<String, String> configMap = loadDailyReportConfigMap();

        DailyReportConfigOutput output = new DailyReportConfigOutput();
        output.setEnabled("true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_ENABLED, "false")));
        output.setCronList(parseCronList(configMap.getOrDefault(CONFIG_KEY_CRON, "0 0 18 * * ?")));
        output.setIncludeDefectDetail("true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_INCLUDE_DEFECT_DETAIL, "true")));
        output.setIncludeSuspended("true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_INCLUDE_SUSPENDED, "true")));

        String webhookUrlsStr = configMap.getOrDefault(CONFIG_KEY_WEBHOOK_URLS, "");
        if (webhookUrlsStr.trim().isEmpty()) {
            output.setWebhookUrls(Collections.emptyList());
        } else {
            List<String> urls = new ArrayList<>();
            for (String url : webhookUrlsStr.split(",")) {
                if (!url.trim().isEmpty()) {
                    urls.add(url.trim());
                }
            }
            output.setWebhookUrls(urls);
        }
        return output;
    }

    /**
     * 更新日报配置
     */
    public void updateConfig(DailyReportConfigUpdateInput input) {
        if (input.getEnabled() != null) {
            upsertConfig(CONFIG_KEY_ENABLED, String.valueOf(input.getEnabled()));
        }
        if (input.getCronList() != null) {
            String joined = input.getCronList().stream()
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.joining(CRON_SEPARATOR));
            if (!joined.isEmpty()) {
                upsertConfig(CONFIG_KEY_CRON, joined);
            }
        }
        if (input.getWebhookUrls() != null) {
            String joined = input.getWebhookUrls().stream()
                    .filter(u -> u != null && !u.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.joining(","));
            upsertConfig(CONFIG_KEY_WEBHOOK_URLS, joined);
        }
        if (input.getIncludeDefectDetail() != null) {
            upsertConfig(CONFIG_KEY_INCLUDE_DEFECT_DETAIL, String.valueOf(input.getIncludeDefectDetail()));
        }
        if (input.getIncludeSuspended() != null) {
            upsertConfig(CONFIG_KEY_INCLUDE_SUSPENDED, String.valueOf(input.getIncludeSuspended()));
        }
        invalidateDailyReportRelatedCaches();
    }

    /**
     * 获取推送 Cron 表达式列表
     */
    public List<String> getPushCronList() {
        Map<String, String> configMap = loadDailyReportConfigMap();
        return parseCronList(configMap.getOrDefault(CONFIG_KEY_CRON, "0 0 18 * * ?"));
    }

    /**
     * 是否启用日报推送
     */
    public boolean isEnabled() {
        Map<String, String> configMap = loadDailyReportConfigMap();
        return "true".equalsIgnoreCase(configMap.getOrDefault(CONFIG_KEY_ENABLED, "false"));
    }

    /**
     * 获取日报调度时区
     */
    public String getScheduleTimezone() {
        long now = System.currentTimeMillis();
        String cached = scheduleTimezoneCache;
        if (cached != null && now < scheduleTimezoneCacheExpiresAtMs) {
            return cached;
        }
        synchronized (this) {
            if (scheduleTimezoneCache != null && System.currentTimeMillis() < scheduleTimezoneCacheExpiresAtMs) {
                return scheduleTimezoneCache;
            }
            String resolved = resolveScheduleTimezoneFromDb();
            scheduleTimezoneCache = resolved;
            scheduleTimezoneCacheExpiresAtMs = System.currentTimeMillis() + DAILY_REPORT_CONFIG_CACHE_TTL_MS;
            return resolved;
        }
    }

    // ==================== 私有方法 ====================

    private List<String> parseCronList(String cronValue) {
        if (cronValue == null || cronValue.trim().isEmpty()) {
            return Collections.singletonList("0 0 18 * * ?");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String cron : cronValue.split(CRON_SEPARATOR)) {
            if (!cron.trim().isEmpty()) {
                unique.add(cron.trim());
            }
        }
        return unique.isEmpty() ? Collections.singletonList("0 0 18 * * ?") : new ArrayList<>(unique);
    }

    private DailyReportSection buildPendingSection(List<DailyReportTicketRow> testingReproduce,
                                                    List<DailyReportTicketRow> processing,
                                                    List<DailyReportTicketRow> pendingVerify) {
        DailyReportSection section = new DailyReportSection();
        section.setTitle("待解决");
        long total = testingReproduce.size() + processing.size() + pendingVerify.size();
        section.setCount(total);

        List<DailyReportSubSection> subs = new ArrayList<>();

        DailyReportSubSection testingSub = new DailyReportSubSection();
        testingSub.setTitle("测试复现中");
        testingSub.setCount(testingReproduce.size());
        testingSub.setTickets(toTicketItems(testingReproduce));
        subs.add(testingSub);

        DailyReportSubSection processingSub = new DailyReportSubSection();
        processingSub.setTitle("处理中");
        processingSub.setCount(processing.size());
        processingSub.setTickets(toTicketItems(processing));
        subs.add(processingSub);

        DailyReportSubSection pendingVerifySub = new DailyReportSubSection();
        pendingVerifySub.setTitle("待简报");
        pendingVerifySub.setCount(pendingVerify.size());
        pendingVerifySub.setTickets(toTicketItems(pendingVerify));
        subs.add(pendingVerifySub);

        section.setSubSections(subs);
        return section;
    }

    private DailyReportSection buildTempResolvedSection(List<DailyReportTicketRow> tempResolved) {
        DailyReportSection section = new DailyReportSection();
        section.setTitle("临时解决");
        section.setCount(tempResolved.size());

        List<DailyReportSubSection> subs = new ArrayList<>();
        if (!tempResolved.isEmpty()) {
            DailyReportSubSection sub = new DailyReportSubSection();
            sub.setTitle("临时解决工单");
            sub.setCount(tempResolved.size());
            sub.setTickets(toTicketItems(tempResolved));
            subs.add(sub);
        }
        section.setSubSections(subs);
        return section;
    }

    private DailyReportSection buildResolvedSection(List<DailyReportStatusRow> closedByDefectType,
                                                     boolean includeDetail) {
        DailyReportSection section = new DailyReportSection();
        section.setTitle("已解决");

        long total = 0;
        List<DailyReportSubSection> subs = new ArrayList<>();

        if (includeDetail) {
            for (DailyReportStatusRow row : closedByDefectType) {
                DailyReportSubSection sub = new DailyReportSubSection();
                sub.setTitle(row.getStatus());
                sub.setCount(safeLong(row.getTotal()));
                sub.setTickets(Collections.emptyList());
                subs.add(sub);
                total += safeLong(row.getTotal());
            }
        } else {
            for (DailyReportStatusRow row : closedByDefectType) {
                total += safeLong(row.getTotal());
            }
        }

        section.setCount(total);
        section.setSubSections(subs);
        return section;
    }

    private DailyReportSection buildSuspendedSection(List<DailyReportTicketRow> suspended) {
        DailyReportSection section = new DailyReportSection();
        section.setTitle("挂起");
        section.setCount(suspended.size());

        List<DailyReportSubSection> subs = new ArrayList<>();
        if (!suspended.isEmpty()) {
            DailyReportSubSection sub = new DailyReportSubSection();
            sub.setTitle("挂起工单");
            sub.setCount(suspended.size());
            sub.setTickets(toTicketItems(suspended));
            subs.add(sub);
        }
        section.setSubSections(subs);
        return section;
    }

    private List<DailyReportTicketItem> toTicketItems(List<DailyReportTicketRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<DailyReportTicketItem> items = new ArrayList<>(rows.size());
        for (DailyReportTicketRow row : rows) {
            DailyReportTicketItem item = new DailyReportTicketItem();
            item.setId(row.getId());
            item.setTicketNo(row.getTicketNo());
            item.setTitle(row.getTitle());
            item.setStatus(row.getStatus());
            item.setStatusLabel(resolveStatusLabel(row.getStatus()));
            item.setPriority(row.getPriority());
            item.setAssigneeName(row.getAssigneeName());
            item.setCategoryName(row.getCategoryName());
            item.setSeverityLevel(row.getSeverityLevel());
            items.add(item);
        }
        return items;
    }

    /**
     * 渲染企微 Markdown 格式日报
     * 参考截图中的日报结构：
     * - 日期标题 + 汇总行
     * - 1、待解决（1.1排查中 / 1.2处理中 / 1.3待简报）
     * - 2、临时解决
     * - 3、已解决（3.1缺陷 / 3.2非缺陷 / 3.3未分类）
     * - 4、挂起
     */
    private String renderMarkdown(DailyReportOutput report, String reportDate) {
        StringBuilder md = new StringBuilder();
        DailyReportSummary summary = report.getSummary();

        md.append("**").append(reportDate).append("线上问题日报**\n");
        md.append("本月问题反馈总数：").append(summary.getTotalFeedbackCount());
        md.append("，今日新增问题：").append(summary.getNewIssueCountToday()).append("个");
        md.append("，待解决问题：").append(summary.getPendingResolveCount()).append("个");
        md.append("，临时解决：").append(summary.getTempResolvedCount()).append("个");
        md.append("，已解决：").append(summary.getResolvedCount()).append("个");
        md.append("，挂起：").append(summary.getSuspendedCount()).append("个\n");

        int sectionIndex = 1;

        if (report.getPendingSection() != null) {
            DailyReportSection pending = report.getPendingSection();
            md.append("\n**").append(sectionIndex).append("、待解决**\n");
            if (pending.getSubSections() != null) {
                int subIndex = 1;
                for (DailyReportSubSection sub : pending.getSubSections()) {
                    md.append("\n**").append(sectionIndex).append(".").append(subIndex).append("、").append(sub.getTitle()).append("：**\n");
                    if (sub.getTickets() != null && !sub.getTickets().isEmpty()) {
                        for (DailyReportTicketItem ticket : sub.getTickets()) {
                            appendTicketLine(md, ticket);
                        }
                    } else {
                        md.append("  无\n");
                    }
                    subIndex++;
                }
            }
            sectionIndex++;
        }

        if (report.getTempResolvedSection() != null) {
            DailyReportSection tempResolved = report.getTempResolvedSection();
            md.append("\n**").append(sectionIndex).append("、临时解决**\n");
            if (tempResolved.getSubSections() != null) {
                int subIndex = 1;
                for (DailyReportSubSection sub : tempResolved.getSubSections()) {
                    md.append("\n").append(sectionIndex).append(".").append(subIndex).append(" ");
                    if (sub.getTickets() != null && !sub.getTickets().isEmpty()) {
                        for (DailyReportTicketItem ticket : sub.getTickets()) {
                            appendTicketLine(md, ticket);
                        }
                    }
                    subIndex++;
                }
            }
            if (tempResolved.getCount() == 0) {
                md.append("  无\n");
            }
            sectionIndex++;
        }

        if (report.getResolvedSection() != null) {
            DailyReportSection resolved = report.getResolvedSection();
            md.append("\n**").append(sectionIndex).append("、已解决**\n");
            if (resolved.getSubSections() != null && !resolved.getSubSections().isEmpty()) {
                int subIndex = 1;
                for (DailyReportSubSection sub : resolved.getSubSections()) {
                    md.append("  ").append(sectionIndex).append(".").append(subIndex)
                            .append("、").append(sub.getTitle())
                            .append("：").append(sub.getCount()).append("个\n");
                    subIndex++;
                }
            } else {
                md.append("  共").append(resolved.getCount()).append("个\n");
            }
            sectionIndex++;
        }

        if (report.getSuspendedSection() != null) {
            DailyReportSection suspended = report.getSuspendedSection();
            md.append("\n**").append(sectionIndex).append("、挂起**\n");
            if (suspended.getSubSections() != null) {
                for (DailyReportSubSection sub : suspended.getSubSections()) {
                    if (sub.getTickets() != null && !sub.getTickets().isEmpty()) {
                        int idx = 1;
                        for (DailyReportTicketItem ticket : sub.getTickets()) {
                            md.append("  ").append(sectionIndex).append(".").append(idx).append(" ");
                            appendTicketLineInline(md, ticket);
                            idx++;
                        }
                    }
                }
            }
            if (suspended.getCount() == 0) {
                md.append("  无\n");
            }
        }

        return md.toString();
    }

    private void appendTicketLine(StringBuilder md, DailyReportTicketItem ticket) {
        md.append("> ");
        if (ticket.getTicketNo() != null) {
            md.append("[").append(ticket.getTicketNo()).append("] ");
        }
        md.append(ticket.getTitle() != null ? ticket.getTitle() : "无标题");
        if (ticket.getAssigneeName() != null && !ticket.getAssigneeName().isEmpty()) {
            md.append(" —— ").append(ticket.getAssigneeName());
        }
        if (ticket.getSeverityLevel() != null && !ticket.getSeverityLevel().isEmpty()) {
            md.append(" (").append(ticket.getSeverityLevel()).append(")");
        }
        md.append("\n");
    }

    private void appendTicketLineInline(StringBuilder md, DailyReportTicketItem ticket) {
        if (ticket.getTicketNo() != null) {
            md.append("[").append(ticket.getTicketNo()).append("] ");
        }
        md.append(ticket.getTitle() != null ? ticket.getTitle() : "无标题");
        if (ticket.getAssigneeName() != null && !ticket.getAssigneeName().isEmpty()) {
            md.append(" —— ").append(ticket.getAssigneeName());
        }
        md.append("\n");
    }

    private String resolveStatusLabel(String statusCode) {
        if (statusCode == null || statusCode.trim().isEmpty()) {
            return "未知";
        }
        TicketStatus ticketStatus = TicketStatus.fromCode(statusCode);
        return ticketStatus != null ? ticketStatus.getLabel() : statusCode;
    }

    /**
     * 带短期 TTL 的日报配置读取，供定时任务高频轮询使用。
     */
    private Map<String, String> loadDailyReportConfigMap() {
        long now = System.currentTimeMillis();
        Map<String, String> cached = dailyReportConfigCache;
        if (cached != null && now < dailyReportConfigCacheExpiresAtMs) {
            return cached;
        }
        synchronized (this) {
            if (dailyReportConfigCache != null && System.currentTimeMillis() < dailyReportConfigCacheExpiresAtMs) {
                return dailyReportConfigCache;
            }
            Map<String, String> fresh = fetchDailyReportConfigMapFromDb();
            dailyReportConfigCache = fresh;
            dailyReportConfigCacheExpiresAtMs = System.currentTimeMillis() + DAILY_REPORT_CONFIG_CACHE_TTL_MS;
            return fresh;
        }
    }

    private void invalidateDailyReportRelatedCaches() {
        dailyReportConfigCache = null;
        dailyReportConfigCacheExpiresAtMs = 0L;
        scheduleTimezoneCache = null;
        scheduleTimezoneCacheExpiresAtMs = 0L;
    }

    private Map<String, String> fetchDailyReportConfigMapFromDb() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, CONFIG_GROUP)
                .eq(SystemConfigPO::getDeleted, 0);
        List<SystemConfigPO> configs = systemConfigMapper.selectList(wrapper);
        Map<String, String> map = new HashMap<>();
        if (configs != null) {
            for (SystemConfigPO config : configs) {
                if (config.getConfigKey() != null && config.getConfigValue() != null) {
                    map.put(config.getConfigKey(), config.getConfigValue());
                }
            }
        }
        return map;
    }

    private String resolveScheduleTimezoneFromDb() {
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
            log.warn("日报推送：读取到非法时区配置，timezone={}，回退默认时区={}", timezone, DEFAULT_TIMEZONE);
            return DEFAULT_TIMEZONE;
        }
    }

    private void upsertConfig(String key, String value) {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigKey, key)
                .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO existing = systemConfigMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setConfigValue(value);
            existing.setUpdateBy("system");
            existing.setUpdateTime(new Date());
            systemConfigMapper.updateById(existing);
        } else {
            SystemConfigPO newConfig = new SystemConfigPO();
            newConfig.setConfigKey(key);
            newConfig.setConfigValue(value);
            newConfig.setConfigGroup(CONFIG_GROUP);
            newConfig.setDescription("日报配置-" + key);
            newConfig.setCreateBy("system");
            newConfig.setUpdateBy("system");
            systemConfigMapper.insert(newConfig);
        }
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(DisplayTimeFormat.TIMEZONE_ID));
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(DisplayTimeFormat.TIMEZONE_ID));
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
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
}
