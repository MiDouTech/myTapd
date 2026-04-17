package com.miduo.cloud.ticket.entity.dto.bugreport;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Bug简报创建入参
 */
@Data
public class BugReportCreateInput implements Serializable {

    private String problemDesc;

    private String logicCauseLevel1;

    private String logicCauseLevel2;

    private String logicCauseDetail;

    private String defectCategory;

    private String introducedProject;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date resolveDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date resolveTime;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date tempResolveDate;

    private String solution;

    private String tempSolution;

    private String impactScope;

    private String severityLevel;

    private Long reporterId;

    private Long reviewerId;

    private String remark;

    @NotEmpty(message = "关联工单不能为空")
    private List<Long> ticketIds;

    private List<Long> responsibleUserIds;

    /**
     * 是否自动预填，默认 true
     */
    private Boolean autoPrefill;
}
