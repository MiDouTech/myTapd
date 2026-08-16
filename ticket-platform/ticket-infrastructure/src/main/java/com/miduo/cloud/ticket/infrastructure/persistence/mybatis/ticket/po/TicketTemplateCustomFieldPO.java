package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_template_custom_field")
public class TicketTemplateCustomFieldPO extends BaseEntity {

    @TableField("template_id")
    private Long templateId;

    @TableField("custom_field_id")
    private Long customFieldId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_enabled")
    private Integer isEnabled;

    @TableField("is_required")
    private Integer isRequired;

    @TableField("placeholder_override")
    private String placeholderOverride;

    @TableField("default_value_override")
    private String defaultValueOverride;
}
