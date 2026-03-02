package com.miduo.cloud.ticket.entity.dto.ticket;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TicketPageInput extends PageInput {

    private String view;

    private String ticketNo;

    private String title;

    private Long categoryId;

    private String status;

    private String priority;

    private Long creatorId;

    private Long assigneeId;

    private String createTimeStart;

    private String createTimeEnd;
}
