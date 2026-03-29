package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 缺陷工单-开发信息更新入参
 */
@Data
public class TicketBugDevInfoInput implements Serializable {

    private String rootCause;

    private String fixSolution;

    private String gitBranch;

    private String impactAssessment;

    private String devRemark;

    /** 计划彻底解决时间 */
    private Date plannedFullResolveAt;
}
