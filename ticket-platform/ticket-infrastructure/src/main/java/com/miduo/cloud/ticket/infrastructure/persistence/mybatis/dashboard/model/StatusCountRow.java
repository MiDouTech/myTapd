package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model;

import lombok.Data;

/**
 * 状态计数行
 */
@Data
public class StatusCountRow {

    private String status;

    private Long total;
}
