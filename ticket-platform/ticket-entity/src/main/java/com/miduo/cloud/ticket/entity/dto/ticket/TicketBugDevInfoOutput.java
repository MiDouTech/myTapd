package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;

/**
 * 缺陷工单-开发信息出参
 */
@Data
public class TicketBugDevInfoOutput implements Serializable {

    private Long ticketId;

    private String rootCause;

    private String fixSolution;

    private String gitBranch;

    private String impactAssessment;

    private String devRemark;
}
