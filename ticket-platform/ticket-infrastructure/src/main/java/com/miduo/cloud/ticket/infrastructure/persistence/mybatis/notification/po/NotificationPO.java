package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知记录持久化对象
 */
@Data
@TableName("notification")
public class NotificationPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("report_id")
    private Long reportId;

    @TableField("type")
    private String type;

    @TableField("channel")
    private String channel;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("is_read")
    private Integer isRead;

    @TableField("read_at")
    private Date readAt;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
