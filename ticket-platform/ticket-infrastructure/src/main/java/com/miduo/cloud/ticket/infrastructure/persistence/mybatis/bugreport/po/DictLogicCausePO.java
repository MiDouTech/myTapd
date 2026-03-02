package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 逻辑归因字典PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dict_logic_cause")
public class DictLogicCausePO extends BaseEntity {

    @TableField("level")
    private Integer level;

    @TableField("name")
    private String name;

    @TableField("parent_id")
    private Long parentId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_active")
    private Integer isActive;
}
