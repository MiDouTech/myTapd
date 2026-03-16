package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

/**
 * 缺陷维度摘要信息（从 Bug 简报关联获取）
 * 工单未关联简报时，此对象为 null
 */
@Data
public class BugSummaryInfoOutput implements Serializable {

    /**
     * 关联简报 ID
     */
    private Long bugReportId;

    /**
     * 简报编号
     */
    private String bugReportNo;

    /**
     * 缺陷划分 code（关联 dict_defect_category）
     */
    private String defectCategory;

    /**
     * 缺陷划分中文名称
     */
    private String defectCategoryLabel;

    /**
     * 是否有效报告原始值：YES / NO
     */
    private String isValidReport;

    /**
     * 是否有效报告中文：是 / 否
     */
    private String isValidReportLabel;

    /**
     * 责任人姓名（多人时以顿号分隔，如：张三、李四）
     */
    private String responsibleUserName;

    /**
     * 是否逾期（计算字段：expectedTime < now && status 非终态）
     */
    private Boolean isOverdue;
}
