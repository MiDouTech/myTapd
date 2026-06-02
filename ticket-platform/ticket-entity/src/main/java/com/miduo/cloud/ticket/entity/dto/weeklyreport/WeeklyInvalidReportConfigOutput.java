package com.miduo.cloud.ticket.entity.dto.weeklyreport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 无效反馈报表配置输出（兼容结构，实际复用日报配置）
 */
@Data
public class WeeklyInvalidReportConfigOutput implements Serializable {

    /**
     * 自动推送开关
     */
    private boolean enabled;

    /**
     * cron 表达式列表
     */
    private List<String> cronList;

    /**
     * 企微群 webhook 地址列表
     */
    private List<String> webhookUrls;

    /**
     * 统计分类 ID 列表
     */
    private List<Long> statCategoryIds;

    /**
     * 明细最大展示条数
     */
    private Integer maxDetailCount;

    /**
     * 调度时区
     */
    private String timezone;
}
