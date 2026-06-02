package com.miduo.cloud.ticket.entity.dto.weeklyreport;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 无效反馈报表配置更新入参（兼容结构，实际复用日报配置）
 */
@Data
public class WeeklyInvalidReportConfigUpdateInput implements Serializable {

    /**
     * 自动推送开关
     */
    @NotNull(message = "推送开关不能为空")
    private Boolean enabled;

    /**
     * cron 表达式列表
     */
    private List<String> cronList;

    /**
     * webhook 地址列表
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
