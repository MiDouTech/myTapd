package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_custom_field_definition")
public class CustomFieldDefinitionPO extends BaseEntity {

    @TableField("field_code")
    private String fieldCode;

    @TableField("field_title")
    private String fieldTitle;

    @TableField("field_type")
    private String fieldType;

    private String description;

    private String placeholder;

    @TableField("default_required")
    private Integer defaultRequired;

    @TableField("default_value")
    private String defaultValue;

    @TableField("validation_config")
    private String validationConfig;

    @TableField("option_config")
    private String optionConfig;

    @TableField("is_active")
    private Integer isActive;
}
