package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("handler_group")
public class HandlerGroupPO extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("leader_id")
    private Long leaderId;

    @TableField("description")
    private String description;

    @TableField("skill_tags")
    private String skillTags;

    @TableField("is_active")
    private Integer isActive;
}
