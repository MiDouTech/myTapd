package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

/**
 * 缺陷工单-客服信息出参
 */
@Data
public class TicketBugCustomerInfoOutput implements Serializable {

    private Long ticketId;

    private String merchantNo;

    private String companyName;

    private String merchantAccount;

    private String problemDesc;

    private String expectedResult;

    private String sceneCode;

    private String problemScreenshot;
}
