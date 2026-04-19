package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工单公开详情输出（无需登录，支持外网访问）
 */
@Data
public class TicketPublicDetailOutput implements Serializable {

    private Long id;

    private String ticketNo;

    private String title;

    private String description;

    private String categoryName;

    private String categoryFullPath;

    private String priority;

    private String priorityLabel;

    private String status;

    private String statusLabel;

    private String creatorName;

    private String assigneeName;

    private String source;

    private String sourceLabel;

    private Date expectedTime;

    private Date resolvedAt;

    private Date closedAt;

    private Date createTime;

    private Date updateTime;

    /**
     * 客服信息（商户编号、公司名称、问题描述等）
     */
    private BugCustomerInfo bugCustomerInfo;

    /**
     * 归档后的 Bug 简报摘要（无归档简报时为 null）
     */
    private ArchivedBugReportSummary archivedBugReport;

    private List<CommentOutput> comments;

    @Data
    public static class BugCustomerInfo implements Serializable {
        private String merchantNo;
        private String companyName;
        private String merchantAccount;
        private String problemDesc;
        private String expectedResult;
        private String sceneCode;
        private String problemScreenshot;
    }

    @Data
    public static class CommentOutput implements Serializable {
        private Long id;
        private String userName;
        private String content;
        private String type;
        private Date createTime;
    }

    @Data
    public static class ArchivedBugReportSummary implements Serializable {
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
        /** 反馈人姓名 */
        private String reporterName;
        private Date reviewedAt;
        private Date updateTime;
        /** 引入项目 */
        private String introducedProject;
        /** 开始时间（简报内填写的开始日期） */
        private Date startDate;
        /** 临时解决时间 */
        private Date tempResolveDate;
        /** 彻底解决日期 */
        private Date resolveDate;
    }
}
