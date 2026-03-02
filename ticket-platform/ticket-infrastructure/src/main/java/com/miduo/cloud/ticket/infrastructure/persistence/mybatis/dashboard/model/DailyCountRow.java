package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model;

import lombok.Data;

/**
 * 日期计数行
 */
@Data
public class DailyCountRow {

    private String day;

    private Long total;
}
