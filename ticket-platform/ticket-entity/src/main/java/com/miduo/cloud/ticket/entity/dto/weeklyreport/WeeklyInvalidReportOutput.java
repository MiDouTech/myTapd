package com.miduo.cloud.ticket.entity.dto.weeklyreport;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 无效反馈报表输出（并入日报管理后默认按月统计）
 */
@Data
public class WeeklyInvalidReportOutput implements Serializable {

    /**
     * 报表日期（yyyy-MM-dd）
     */
    private String reportDate;

    /**
     * 统计区间展示文案
     */
    private String weekRangeLabel;

    /**
     * 汇总信息
     */
    private WeeklyInvalidReportSummary summary;

    /**
     * 按反馈人统计
     */
    private List<WeeklyInvalidReporterItem> reporterStats;

    /**
     * 无效反馈明细
     */
    private List<WeeklyInvalidTicketItem> ticketDetails;

    /**
     * 企微 markdown 文本
     */
    private String markdownContent;

    @Data
    public static class WeeklyInvalidReportSummary implements Serializable {
        private long invalidTotalCount;
        private long reporterCount;
        private long detailDisplayCount;
        private long detailLimitCount;
    }

    @Data
    public static class WeeklyInvalidReporterItem implements Serializable {
        private Long reporterId;
        private String reporterName;
        private long invalidCount;
    }

    @Data
    public static class WeeklyInvalidTicketItem implements Serializable {
        private Long id;
        private String ticketNo;
        private String title;
        private Long reporterId;
        private String reporterName;
        private Date closedTime;
    }
}
