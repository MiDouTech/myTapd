package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 告警事件日志持久化对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_event_log")
public class AlertEventLogPO extends BaseEntity {

    @TableField("event_hash")
    private String eventHash;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("rule_name")
    private String ruleName;

    @TableField("severity")
    private Integer severity;

    @TableField("target_ident")
    private String targetIdent;

    @TableField("trigger_value")
    private String triggerValue;

    @TableField("trigger_time")
    private Date triggerTime;

    @TableField("is_recovered")
    private Boolean isRecovered;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("raw_payload")
    private String rawPayload;

    @TableField("process_result")
    private String processResult;
}
