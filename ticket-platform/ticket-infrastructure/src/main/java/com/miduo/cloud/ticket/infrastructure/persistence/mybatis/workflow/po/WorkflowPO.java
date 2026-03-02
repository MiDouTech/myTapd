package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow")
public class WorkflowPO extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("mode")
    private String mode;

    @TableField("description")
    private String description;

    @TableField("states")
    private String states;

    @TableField("transitions")
    private String transitions;

    @TableField("is_builtin")
    private Integer isBuiltin;

    @TableField("is_active")
    private Integer isActive;
}
