package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Data
public class TicketPriorityUpdateInput implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "优先级不能为空")
    @Pattern(regexp = "(?i)urgent|high|medium|low", message = "优先级不合法")
    private String priority;
}
