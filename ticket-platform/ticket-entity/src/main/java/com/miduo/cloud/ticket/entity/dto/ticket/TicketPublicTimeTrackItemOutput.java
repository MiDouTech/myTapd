package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工单公开详情中的时间追踪摘要项（不含字段级变更明细）
 */
@Data
public class TicketPublicTimeTrackItemOutput implements Serializable {

    private Long id;

    private String userName;

    private String action;

    private String actionLabel;

    private String fromStatus;

    private String fromStatusLabel;

    private String toStatus;

    private String toStatusLabel;

    private String fromUserName;

    private String toUserName;

    private String remark;

    private Date timestamp;
}
