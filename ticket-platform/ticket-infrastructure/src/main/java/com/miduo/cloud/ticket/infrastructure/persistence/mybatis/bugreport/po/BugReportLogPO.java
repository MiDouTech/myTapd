package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Bug简报操作日志PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bug_report_log")
public class BugReportLogPO extends BaseEntity {

    @TableField("report_id")
    private Long reportId;

    @TableField("user_id")
    private Long userId;

    @TableField("action")
    private String action;

    @TableField("old_status")
    private String oldStatus;

    @TableField("new_status")
    private String newStatus;

    @TableField("remark")
    private String remark;
}
