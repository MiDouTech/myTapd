package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * SLA策略持久化对象
 */
@Data
@TableName("sla_policy")
public class SlaPolicyPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
