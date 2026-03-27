package com.miduo.cloud.ticket.entity.dto.dailyreport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 日报配置输出对象
 */
@Data
public class DailyReportConfigOutput implements Serializable {

    /**
     * 日报推送开关
     */
    private boolean enabled;

    /**
     * Cron 表达式
     */
    private String cron;

    /**
     * 企微群 Webhook 地址列表
     */
    private List<String> webhookUrls;

    /**
     * 是否包含缺陷明细分类
     */
    private boolean includeDefectDetail;

    /**
     * 是否包含挂起工单列表
     */
    private boolean includeSuspended;
}
