package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警事件日志 - 分页查询请求
 */
@Data
public class AlertEventLogPageInput implements Serializable {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String ruleName;

    private String targetIdent;

    private String processResult;
}
