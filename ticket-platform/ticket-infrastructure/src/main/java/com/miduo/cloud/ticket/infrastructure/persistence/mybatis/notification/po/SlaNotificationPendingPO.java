package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * SLA 通知延迟发送队列
 */
@Data
@TableName("sla_notification_pending")
public class SlaNotificationPendingPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("notification_type")
    private String notificationType;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("detail_link")
    private String detailLink;

    @TableField("receiver_user_ids")
    private String receiverUserIds;

    @TableField("mention_user_ids")
    private String mentionUserIds;

    @TableField("scheduled_send_at")
    private Date scheduledSendAt;

    @TableField("sent_at")
    private Date sentAt;

    @TableField("status")
    private String status;

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
