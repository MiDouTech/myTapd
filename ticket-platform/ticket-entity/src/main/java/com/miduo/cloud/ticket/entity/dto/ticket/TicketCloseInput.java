package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

@Data
public class TicketCloseInput implements Serializable {

    private String remark;

    /** 处理结论（公开可见；优先于 remark 写入 resolution_summary） */
    private String resolutionSummary;
}
