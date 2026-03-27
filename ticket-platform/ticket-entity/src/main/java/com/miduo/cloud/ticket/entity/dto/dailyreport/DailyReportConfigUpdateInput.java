package com.miduo.cloud.ticket.entity.dto.dailyreport;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 日报配置更新输入对象
 */
@Data
public class DailyReportConfigUpdateInput implements Serializable {

    /**
     * 日报推送开关
     */
    @NotNull(message = "推送开关不能为空")
    private Boolean enabled;

    /**
     * Cron 表达式列表（支持多个推送时间点）
     */
    private List<String> cronList;

    /**
     * 企微群 Webhook 地址列表
     */
    private List<String> webhookUrls;

    /**
     * 是否包含缺陷明细分类
     */
    private Boolean includeDefectDetail;

    /**
     * 是否包含挂起工单列表
     */
    private Boolean includeSuspended;
}
