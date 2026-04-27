package com.miduo.cloud.ticket.entity.dto.ticket;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TicketPageInput extends PageInput {

    private String view;

    private String ticketNo;

    private String title;

    /** 统一关键词：同时模糊匹配工单编号与标题（与列表路由 q 对应） */
    private String keyword;

    private Long categoryId;

    private String status;

    /**
     * 多状态筛选（与 {@link #status} 合并去重）；任一匹配即命中（SQL IN）。
     * 兼容 OpenAPI 等仍传单个 {@code status} 的调用方。
     */
    private List<String> statuses;

    private String priority;

    private Long creatorId;

    private Long assigneeId;

    private String createTimeStart;

    private String createTimeEnd;

    /**
     * SLA状态过滤：BREACHED-已超时 / WARNING-预警中 / NORMAL-正常
     * 仅传入时生效，用于仪表盘点击SLA超时卡片跳转过滤
     */
    private String slaStatus;

    /**
     * 为 true 时仅返回可关联 Bug 简报的工单：临时解决、已完成（不含非缺陷关闭的已关闭），
     * 且排除已关联「已归档」Bug 简报的工单（避免重复闭环）
     */
    private Boolean linkableForBugReport;
}
