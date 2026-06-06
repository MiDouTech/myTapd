package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 待我审批列表输出
 */
@Data
public class ApprovalPendingListOutput implements Serializable {

    /** 待审批总数（角标用） */
    private Long totalCount;

    /** 当前页数据 */
    private List<PendingItem> items;

    @Data
    public static class PendingItem implements Serializable {

        /** 审批任务ID */
        private Long taskId;

        /** 工单ID */
        private Long ticketId;

        /** 工单编号 */
        private String ticketNo;

        /** 工单标题 */
        private String ticketTitle;

        /** 工单当前状态码 */
        private String ticketStatus;

        /** 工单当前状态名称 */
        private String ticketStatusLabel;

        /** 工单提交人名称 */
        private String creatorName;

        /** 审批节点名称 */
        private String nodeName;

        /** 审批模式 */
        private String approveMode;

        /** 等待审批时长（距任务创建时间，单位：分钟） */
        private Long waitMinutes;

        /** 任务创建时间 */
        private Date createTime;

        /** 超时截止时间 */
        private Date dueTime;

        /** 是否超时 */
        private Boolean isOverdue;
    }
}
