package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Bug简报主表PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bug_report")
public class BugReportPO extends BaseEntity {

    @TableField("report_no")
    private String reportNo;

    @TableField("status")
    private String status;

    @TableField("problem_desc")
    private String problemDesc;

    @TableField("logic_cause_level1")
    private String logicCauseLevel1;

    @TableField("logic_cause_level2")
    private String logicCauseLevel2;

    @TableField("logic_cause_detail")
    private String logicCauseDetail;

    @TableField("defect_category")
    private String defectCategory;

    @TableField("introduced_project")
    private String introducedProject;

    @TableField("start_date")
    private Date startDate;

    @TableField("resolve_date")
    private Date resolveDate;

    @TableField("temp_resolve_date")
    private Date tempResolveDate;

    @TableField("solution")
    private String solution;

    @TableField("temp_solution")
    private String tempSolution;

    @TableField("impact_scope")
    private String impactScope;

    @TableField("severity_level")
    private String severityLevel;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("remark")
    private String remark;

    @TableField("submitted_at")
    private Date submittedAt;

    @TableField("reviewed_at")
    private Date reviewedAt;

    @TableField("review_comment")
    private String reviewComment;

    @TableField("created_by_user_id")
    private Long createdByUserId;
}
