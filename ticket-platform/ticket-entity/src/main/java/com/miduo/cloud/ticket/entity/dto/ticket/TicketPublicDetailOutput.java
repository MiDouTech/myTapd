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

    /**
     * 测试受理人：最近一次「待测试受理 -> 测试复现中」流转操作人姓名（历史数据回退到测试节点处理人），无则 null
     */
    private String testFollowAssigneeNames;

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

    /**
     * SLA计时器（公开页倒计时展示）
     */
    private List<SlaTimerOutput> slaTimers;

    /**
     * SLA工作时间配置（公开页按工作时间倒计时）
     */
    private WorkingTimeOutput workingTime;

    /**
     * 服务器返回时间，用于公开页计算已经过的工作秒数
     */
    private Date serverTime;

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

    @Data
    public static class SlaTimerOutput implements Serializable {
        private Long id;
        private String timerType;
        private String timerTypeLabel;
        private String status;
        private String statusLabel;
        private Integer thresholdMinutes;
        private Integer elapsedMinutes;
        private Long elapsedSeconds;
        private Long remainingSeconds;
        private Date deadline;
        private Boolean breached;
    }

    @Data
    public static class WorkingTimeOutput implements Serializable {
        private String workTimeStart;
        private String workTimeEnd;
        private List<Integer> workingDays;
    }
}
