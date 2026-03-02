package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工单节点耗时统计出参
 */
@Data
public class TicketNodeDurationOutput implements Serializable {

    private Long ticketId;

    private List<NodeItem> nodes;

    @Data
    public static class NodeItem implements Serializable {
        private Long id;
        private String nodeName;
        private Long assigneeId;
        private String assigneeName;
        private String assigneeRole;
        private Date arriveAt;
        private Date firstReadAt;
        private Date startProcessAt;
        private Date leaveAt;
        private Long waitDurationSec;
        private Long processDurationSec;
        private Long totalDurationSec;
    }
}
