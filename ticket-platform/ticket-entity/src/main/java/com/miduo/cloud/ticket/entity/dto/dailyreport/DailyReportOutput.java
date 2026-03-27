package com.miduo.cloud.ticket.entity.dto.dailyreport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 日报输出对象（包含结构化数据 + 预渲染的 Markdown 文本）
 */
@Data
public class DailyReportOutput implements Serializable {

    /**
     * 日报日期（yyyy-MM-dd 格式）
     */
    private String reportDate;

    /**
     * 汇总信息
     */
    private DailyReportSummary summary;

    /**
     * 待解决工单（排查中 / 处理中 / 待简报）
     */
    private DailyReportSection pendingSection;

    /**
     * 临时解决工单
     */
    private DailyReportSection tempResolvedSection;

    /**
     * 已解决工单（缺陷 / 非缺陷 / 未分类）
     */
    private DailyReportSection resolvedSection;

    /**
     * 挂起工单
     */
    private DailyReportSection suspendedSection;

    /**
     * 预渲染的企微 Markdown 文本
     */
    private String markdownContent;

    /**
     * 日报汇总统计
     */
    @Data
    public static class DailyReportSummary implements Serializable {
        private long totalFeedbackCount;
        private long pendingResolveCount;
        private long tempResolvedCount;
        private long resolvedCount;
        private long suspendedCount;
    }

    /**
     * 日报分区
     */
    @Data
    public static class DailyReportSection implements Serializable {
        private String title;
        private long count;
        private List<DailyReportSubSection> subSections;
    }

    /**
     * 日报子分区
     */
    @Data
    public static class DailyReportSubSection implements Serializable {
        private String title;
        private long count;
        private List<DailyReportTicketItem> tickets;
    }

    /**
     * 日报工单条目
     */
    @Data
    public static class DailyReportTicketItem implements Serializable {
        private Long id;
        private String ticketNo;
        private String title;
        private String status;
        private String statusLabel;
        private String priority;
        private String assigneeName;
        private String categoryName;
        private String severityLevel;
    }
}
