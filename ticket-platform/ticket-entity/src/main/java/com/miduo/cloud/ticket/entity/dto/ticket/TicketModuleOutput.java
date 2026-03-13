package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

/**
 * 工单模块出参
 */
@Data
public class TicketModuleOutput implements Serializable {

    private Long id;

    private String name;

    private Integer sort;
}
