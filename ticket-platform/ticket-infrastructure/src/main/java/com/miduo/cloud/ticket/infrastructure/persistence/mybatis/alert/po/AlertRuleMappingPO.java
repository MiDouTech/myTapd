package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 告警规则映射配置持久化对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_rule_mapping")
public class AlertRuleMappingPO extends BaseEntity {

    @TableField("rule_name")
    private String ruleName;

    @TableField("match_mode")
    private String matchMode;

    @TableField("category_id")
    private Long categoryId;

    @TableField("priority_p1")
    private String priorityP1;

    @TableField("priority_p2")
    private String priorityP2;

    @TableField("priority_p3")
    private String priorityP3;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("dedup_window_minutes")
    private Integer dedupWindowMinutes;

    @TableField("enabled")
    private Boolean enabled;
}
