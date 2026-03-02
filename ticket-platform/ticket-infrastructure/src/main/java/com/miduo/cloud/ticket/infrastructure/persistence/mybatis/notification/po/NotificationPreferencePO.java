package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户通知偏好持久化对象
 */
@Data
@TableName("notification_preference")
public class NotificationPreferencePO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("event_type")
    private String eventType;

    @TableField("site_enabled")
    private Integer siteEnabled;

    @TableField("wecom_enabled")
    private Integer wecomEnabled;

    @TableField("email_enabled")
    private Integer emailEnabled;

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
