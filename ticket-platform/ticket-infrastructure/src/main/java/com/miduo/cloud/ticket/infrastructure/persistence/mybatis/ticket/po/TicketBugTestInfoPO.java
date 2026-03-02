package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 缺陷工单测试信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_bug_test_info")
public class TicketBugTestInfoPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("reproduce_env")
    private String reproduceEnv;

    @TableField("reproduce_steps")
    private String reproduceSteps;

    @TableField("actual_result")
    private String actualResult;

    @TableField("impact_scope")
    private String impactScope;

    @TableField("severity_level")
    private String severityLevel;

    @TableField("module_name")
    private String moduleName;

    @TableField("reproduce_screenshot")
    private String reproduceScreenshot;

    @TableField("test_remark")
    private String testRemark;
}
