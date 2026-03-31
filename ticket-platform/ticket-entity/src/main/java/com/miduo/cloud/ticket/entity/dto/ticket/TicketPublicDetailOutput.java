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

    /**
     * 处理结论（终态工单，公开可读）
     */
    private String resolutionSummary;

    private Date createTime;

    private Date updateTime;

    /**
     * 工单描述是否与客服「问题描述」重复（前端用于折叠展示）
     */
    private Boolean descriptionDuplicateOfProblemDesc;

    /**
     * 客服信息（商户编号、公司名称、问题描述等）
     */
    private BugCustomerInfo bugCustomerInfo;

    private List<CommentOutput> comments;

    /**
     * 处理动态-系统事件（与 comments 合并展示时按时间排序）
     */
    private List<TicketPublicActivityOutput> activities;

    /**
     * 附件列表（公开只读）
     */
    private List<TicketPublicAttachmentOutput> publicAttachments;

    @Data
    public static class BugCustomerInfo implements Serializable {
        private String merchantNo;
        private String companyName;
        private String merchantAccount;
        private String problemDesc;
        private String expectedResult;
        private String sceneCode;
        private String problemScreenshot;

        /** 接口排障扩展（URL 已脱敏） */
        private BugTroubleshooting troubleshooting;
    }

    @Data
    public static class BugTroubleshooting implements Serializable {
        private String requestUrl;
        private String httpStatus;
        private String bizErrorCode;
        private String traceId;
        private Date occurredAt;
        private String clientType;
        private String clientTypeLabel;
    }

    @Data
    public static class CommentOutput implements Serializable {
        private Long id;
        private String userName;
        private String content;
        private String type;
        private Date createTime;
    }
}
