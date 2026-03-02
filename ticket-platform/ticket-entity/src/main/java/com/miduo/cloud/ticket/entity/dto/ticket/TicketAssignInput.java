package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class TicketAssignInput implements Serializable {

    @NotNull(message = "处理人ID不能为空")
    private Long assigneeId;

    private String remark;
}
