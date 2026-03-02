package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Bug简报更新入参
 */
@Data
public class BugReportUpdateInput implements Serializable {

    private String problemDesc;

    private String logicCauseLevel1;

    private String logicCauseLevel2;

    private String logicCauseDetail;

    private String defectCategory;

    private String introducedProject;

    private Date startDate;

    private Date resolveDate;

    private String solution;

    private String impactScope;

    private String severityLevel;

    private Long reporterId;

    private Long reviewerId;

    private String remark;

    private List<Long> ticketIds;

    private List<Long> responsibleUserIds;

    /**
     * 是否自动预填，默认 false
     */
    private Boolean autoPrefill;
}
