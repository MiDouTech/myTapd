package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单模块PO（用于测试信息-所属模块下拉选择的自定义选项）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_module")
public class TicketModulePO extends BaseEntity {

    @TableField("name")
    private String name;

    @TableField("sort")
    private Integer sort;
}
