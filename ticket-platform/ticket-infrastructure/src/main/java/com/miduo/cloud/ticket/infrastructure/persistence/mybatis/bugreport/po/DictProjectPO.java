package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目字典PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dict_project")
public class DictProjectPO extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("is_active")
    private Integer isActive;
}
