package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

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

    private String troubleshootRequestUrl;

    private String troubleshootHttpStatus;

    private String troubleshootBizErrorCode;

    private String troubleshootTraceId;

    private Date troubleshootOccurredAt;

    private String troubleshootClientType;

    private String troubleshootClientTypeLabel;
}
