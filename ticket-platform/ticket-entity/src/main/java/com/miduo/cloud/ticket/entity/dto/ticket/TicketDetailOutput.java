package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class TicketDetailOutput implements Serializable {

    private Long id;

    private String ticketNo;

    private String title;

    private String description;

    private Long categoryId;

    private String categoryName;

    private String categoryFullPath;

    private Long templateId;

    private String templateName;

    private Long workflowId;

    private String priority;

    private String priorityLabel;

    private String status;

    private String statusLabel;

    private Long creatorId;

    private String creatorName;

    private Long assigneeId;

    private String assigneeName;

    private String source;

    private String sourceLabel;

    private Date expectedTime;

    private Date resolvedAt;

    private Date closedAt;

    private Date createTime;

    private Date updateTime;

    private Map<String, String> customFields;

    private List<AttachmentOutput> attachments;

    private List<CommentOutput> comments;

    private List<LogOutput> logs;

    private List<BugReportOutput> bugReports;

    private Boolean isFollowed;

    @Data
    public static class AttachmentOutput implements Serializable {
        private Long id;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String fileType;
        private Long uploadedBy;
        private String uploadedByName;
        private Date createTime;
    }

    @Data
    public static class CommentOutput implements Serializable {
        private Long id;
        private Long userId;
        private String userName;
        private String userAvatar;
        private String content;
        private String type;
        private Date createTime;
    }

    @Data
    public static class LogOutput implements Serializable {
        private Long id;
        private Long userId;
        private String userName;
        private String action;
        private String actionLabel;
        private String oldValue;
        private String newValue;
        private String remark;
        private Date createTime;
    }

    @Data
    public static class BugReportOutput implements Serializable {
        private Long id;
        private String reportNo;
        private String status;
        private String statusLabel;
        private Integer isAutoCreated;
        private Date createTime;
    }
}
