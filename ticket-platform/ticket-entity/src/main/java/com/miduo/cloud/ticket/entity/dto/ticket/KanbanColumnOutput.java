package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 看板列数据
 */
@Data
public class KanbanColumnOutput implements Serializable {

    private String status;

    private String statusLabel;

    private List<KanbanTicketOutput> tickets;
}
