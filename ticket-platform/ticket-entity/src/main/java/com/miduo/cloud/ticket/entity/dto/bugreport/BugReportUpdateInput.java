package com.miduo.cloud.ticket.entity.dto.bugreport;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    private List<Long> ticketIds;

    private List<Long> responsibleUserIds;

    /**
     * 是否自动预填，默认 false
     */
    private Boolean autoPrefill;

    /**
     * 为 true 时清空「解决时间」，用于关联工单进入临时解决后保存
     */
    private Boolean clearResolveTime;

    /**
     * 为 true 时清空临时/彻底解决相关四个字段，用于关联工单已处理完成且只保留解决时间
     */
    private Boolean clearThoroughAndTempResolution;
}
