package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 工单时间追踪记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_time_track")
public class TicketTimeTrackPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_role")
    private String userRole;

    @TableField("action")
    private String action;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("from_user_id")
    private Long fromUserId;

    @TableField("to_user_id")
    private Long toUserId;

    @TableField("remark")
    private String remark;

    @TableField("is_first_read")
    private Integer isFirstRead;

    @TableField("timestamp")
    private Date timestamp;
}
