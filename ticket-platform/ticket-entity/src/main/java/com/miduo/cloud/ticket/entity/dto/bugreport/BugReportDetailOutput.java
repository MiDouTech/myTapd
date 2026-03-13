package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Bug简报详情
 */
@Data
public class BugReportDetailOutput implements Serializable {

    private Long id;

    private String reportNo;

    private String status;

    private String statusLabel;

    private String problemDesc;

    private String logicCauseLevel1;

    private String logicCauseLevel2;

    private String logicCauseDetail;

    private String defectCategory;

    private String introducedProject;

    private Date startDate;

    private Date resolveDate;

    private Date tempResolveDate;

    private String solution;

    private String tempSolution;

    private String impactScope;

    private String severityLevel;

    private Long reporterId;

    private String reporterName;

    private Long reviewerId;

    private String reviewerName;

    private String remark;

    private Date submittedAt;

    private Date reviewedAt;

    private String reviewComment;

    private Long createdByUserId;

    private Date createTime;

    private Date updateTime;

    private List<ResponsibleUserOutput> responsibleUsers;

    private List<RelatedTicketOutput> tickets;

    private List<LogOutput> logs;

    private List<AttachmentOutput> attachments;

    @Data
    public static class ResponsibleUserOutput implements Serializable {
        private Long userId;
        private String userName;
    }

    @Data
    public static class RelatedTicketOutput implements Serializable {
        private Long ticketId;
        private String ticketNo;
        private String title;
        private String status;
        private Integer isAutoCreated;
    }

    @Data
    public static class LogOutput implements Serializable {
        private Long id;
        private Long userId;
        private String userName;
        private String action;
        private String oldStatus;
        private String newStatus;
        private String remark;
        private Date createTime;
    }

    @Data
    public static class AttachmentOutput implements Serializable {
        private Long id;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private Long uploadedBy;
        private String uploadedByName;
        private Date createTime;
    }
}
