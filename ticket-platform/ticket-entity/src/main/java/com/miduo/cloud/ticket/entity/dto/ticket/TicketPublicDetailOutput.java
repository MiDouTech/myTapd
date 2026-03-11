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

    private List<CommentOutput> comments;

    @Data
    public static class CommentOutput implements Serializable {
        private Long id;
        private String userName;
        private String content;
        private String type;
        private Date createTime;
    }
}
