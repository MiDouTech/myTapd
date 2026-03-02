package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工单时间追踪链路出参
 */
@Data
public class TicketTimeTrackOutput implements Serializable {

    private Long ticketId;

    private List<TrackItem> tracks;

    @Data
    public static class TrackItem implements Serializable {
        private Long id;
        private Long userId;
        private String userName;
        private String userRole;
        private String action;
        private String actionLabel;
        private String fromStatus;
        private String toStatus;
        private Long fromUserId;
        private String fromUserName;
        private Long toUserId;
        private String toUserName;
        private String remark;
        private Boolean isFirstRead;
        private Date timestamp;
    }
}
