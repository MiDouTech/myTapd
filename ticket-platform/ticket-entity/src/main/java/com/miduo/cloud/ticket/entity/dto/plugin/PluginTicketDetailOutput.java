package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 插件内工单详情。
 */
@Data
public class PluginTicketDetailOutput implements Serializable {

    private Long ticketId;
    private String ticketNo;
    private String title;
    private String description;
    private String status;
    private String statusLabel;
    private String priority;
    private Date createTime;
    private Date updateTime;
    private Integer urgeCount;
    private Boolean canUrge;
    private String urgeDisabledReason;
    private List<Message> messages;

    @Data
    public static class Message implements Serializable {
        private Long id;
        private String userName;
        private String content;
        private String type;
        private Date createTime;
    }
}
