package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.util.List;

/** 自定义工单查询的单个条件。 */
@Data
public class TicketCustomConditionInput {

    private String field;

    private String operator;

    private List<String> values;
}
