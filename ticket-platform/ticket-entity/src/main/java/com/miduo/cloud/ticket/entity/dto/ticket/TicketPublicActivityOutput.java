package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工单公开详情-处理动态（系统事件，不含用户评论）
 */
@Data
public class TicketPublicActivityOutput implements Serializable {

    private Long id;

    /** 事件类型：如 STRUCT_CREATE、STRUCT_TRANSIT、STRUCT_ASSIGN、STRUCT_TRANSFER */
    private String eventType;

    private String eventTypeLabel;

    private String summary;

    private String operatorName;

    private Date createTime;
}
