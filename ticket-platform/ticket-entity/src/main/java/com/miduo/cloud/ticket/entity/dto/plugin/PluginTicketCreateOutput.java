package com.miduo.cloud.ticket.entity.dto.plugin;

import lombok.Data;

import java.io.Serializable;

/**
 * 插件创建工单输出
 */
@Data
public class PluginTicketCreateOutput implements Serializable {

    private Long ticketId;

    private String ticketNo;

    private String status;

    private String publicUrl;
}
