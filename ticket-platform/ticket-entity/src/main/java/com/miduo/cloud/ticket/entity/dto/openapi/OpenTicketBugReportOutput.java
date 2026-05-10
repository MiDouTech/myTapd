package com.miduo.cloud.ticket.entity.dto.openapi;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 开放接口：按工单编号查询 Bug 简报输出
 */
@Data
public class OpenTicketBugReportOutput implements Serializable {

    /**
     * 工单编号
     */
    private String ticketNo;

    /**
     * 最新归档 Bug 简报（无归档简报时为 null）
     */
    private ArchivedBugReport bugReport;

    @Data
    public static class ArchivedBugReport implements Serializable {
        private Long id;
        private String reportNo;
        private String status;
        private String statusLabel;
        private String defectCategory;
        private String severityLevel;
        private String logicCauseLevel1;
        private String logicCauseLevel2;
        private String logicCauseDetail;
        private String problemDesc;
        private String impactScope;
        private String solution;
        private String tempSolution;
        private String responsibleUserNames;
        private String reporterName;
        private String introducedProject;
        private Date startDate;
        private Date tempResolveDate;
        private Date resolveDate;
        private Date reviewedAt;
        private Date updateTime;
    }
}
