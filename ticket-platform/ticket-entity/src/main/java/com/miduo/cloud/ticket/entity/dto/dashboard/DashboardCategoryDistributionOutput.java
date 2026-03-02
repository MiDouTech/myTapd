package com.miduo.cloud.ticket.entity.dto.dashboard;

import lombok.Data;

import java.io.Serializable;

/**
 * 分类分布数据
 */
@Data
public class DashboardCategoryDistributionOutput implements Serializable {

    private Long categoryId;

    private String categoryName;

    private Long ticketCount;

    private Double percentage;
}
