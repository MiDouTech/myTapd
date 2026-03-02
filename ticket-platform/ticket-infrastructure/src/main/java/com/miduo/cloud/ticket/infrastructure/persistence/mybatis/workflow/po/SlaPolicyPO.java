package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sla_policy")
public class SlaPolicyPO extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("priority")
    private String priority;

    @TableField("response_time")
    private Integer responseTime;

    @TableField("resolve_time")
    private Integer resolveTime;

    @TableField("warning_pct")
    private Integer warningPct;

    @TableField("critical_pct")
    private Integer criticalPct;

    @TableField("description")
    private String description;

    @TableField("is_active")
    private Integer isActive;
}
