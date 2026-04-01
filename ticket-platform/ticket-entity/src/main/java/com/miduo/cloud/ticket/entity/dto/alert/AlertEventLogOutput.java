package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 告警事件日志 - 输出
 */
@Data
public class AlertEventLogOutput implements Serializable {

    private Long id;

    private String eventHash;

    private Long ruleId;

    private String ruleName;

    private Integer severity;

    private String targetIdent;

    private String triggerValue;

    private Date triggerTime;

    private Boolean isRecovered;

    private Long ticketId;

    private String ticketNo;

    private String processResult;

    private Date createTime;
}
