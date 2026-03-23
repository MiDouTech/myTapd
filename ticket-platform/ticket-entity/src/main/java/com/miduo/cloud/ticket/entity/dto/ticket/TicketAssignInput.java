package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TicketAssignInput implements Serializable {

    /**
     * 单个处理人（兼容旧客户端）
     */
    private Long assigneeId;

    /**
     * 多名处理人，首位为主处理人；与 assigneeId 至少填一种
     */
    private List<Long> assigneeIds;

    private String remark;
}
