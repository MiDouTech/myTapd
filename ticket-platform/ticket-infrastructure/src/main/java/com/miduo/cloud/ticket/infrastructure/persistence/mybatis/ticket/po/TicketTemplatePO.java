package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_template")
public class TicketTemplatePO extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("category_id")
    private Long categoryId;

    @TableField("fields_config")
    private String fieldsConfig;

    @TableField("description")
    private String description;

    @TableField("is_active")
    private Integer isActive;
}
