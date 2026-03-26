package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_log")
public class TicketLogPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField(value = "user_id", insertStrategy = FieldStrategy.IGNORED)
    private Long userId = 0L;

    @TableField("action")
    private String action;

    @TableField("old_value")
    private String oldValue;

    @TableField("new_value")
    private String newValue;

    @TableField("remark")
    private String remark;
}
