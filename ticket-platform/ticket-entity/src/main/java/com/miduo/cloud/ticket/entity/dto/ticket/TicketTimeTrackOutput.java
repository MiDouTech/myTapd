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

    /**
     * 未能关联到时间链节点的字段变更批次（如仅保存客服/测试/开发信息、无对应轨迹点）
     */
    private List<BugChangeHistoryOutput> standaloneFieldChanges;

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

        /**
         * 与该轨迹点在时间上关联的字段填写/变更明细（来自 ticket_log 变更历史）
         */
        private List<BugFieldChangeItem> fieldChanges;
    }
}
