package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 缺陷工单开发信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_bug_dev_info")
public class TicketBugDevInfoPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("root_cause")
    private String rootCause;

    @TableField("fix_solution")
    private String fixSolution;

    @TableField("git_branch")
    private String gitBranch;

    @TableField("impact_assessment")
    private String impactAssessment;

    @TableField("dev_remark")
    private String devRemark;
}
