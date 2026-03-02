package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_follower")
public class TicketFollowerPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("user_id")
    private Long userId;
}
