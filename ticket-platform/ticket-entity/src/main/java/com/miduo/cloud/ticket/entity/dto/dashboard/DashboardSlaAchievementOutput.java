package com.miduo.cloud.ticket.entity.dto.dashboard;

import lombok.Data;

import java.io.Serializable;

/**
 * SLA达成统计
 */
@Data
public class DashboardSlaAchievementOutput implements Serializable {

    private Long totalCount;

    private Long achievedCount;

    private Long breachedCount;

    private Double achievementRate;
}
