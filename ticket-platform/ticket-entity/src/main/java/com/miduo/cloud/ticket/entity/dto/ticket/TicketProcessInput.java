package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class TicketProcessInput implements Serializable {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    private Long targetUserId;

    private String remark;

    /** 流转至终态时的处理结论（写入 resolution_summary） */
    private String resolutionSummary;
}
