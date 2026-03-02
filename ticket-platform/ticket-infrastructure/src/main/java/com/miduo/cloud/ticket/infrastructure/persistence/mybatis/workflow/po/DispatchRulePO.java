package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分派规则PO
 */
@Data
@TableName("dispatch_rule")
public class DispatchRulePO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("category_id")
    private Long categoryId;

    @TableField("strategy")
    private String strategy;

    @TableField("target_group_id")
    private Long targetGroupId;

    @TableField("target_user_id")
    private Long targetUserId;

    @TableField("rule_config")
    private String ruleConfig;

    @TableField("priority_order")
    private Integer priorityOrder;

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
