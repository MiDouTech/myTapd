package com.miduo.cloud.ticket.entity.dto.notification;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知输出DTO
 */
@Data
public class NotificationOutput implements Serializable {

    private Long id;

    private Long userId;

    private Long ticketId;

    private Long reportId;

    private String type;

    private String typeLabel;

    private String channel;

    private String channelLabel;

    private String title;

    private String content;

    private Integer isRead;

    private Date readAt;

    private Date createTime;
}
