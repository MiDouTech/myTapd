package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

@Data
public class TicketCloseInput implements Serializable {

    private String remark;
}
