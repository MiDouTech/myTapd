package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

/**
 * 缺陷工单-测试信息出参
 */
@Data
public class TicketBugTestInfoOutput implements Serializable {

    private Long ticketId;

    private String reproduceEnv;

    private String reproduceSteps;

    private String actualResult;

    private String impactScope;

    private String severityLevel;

    private String moduleName;

    private String reproduceScreenshot;

    private String testRemark;
}
