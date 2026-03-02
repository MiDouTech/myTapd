package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Bug简报责任人关联PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bug_report_responsible")
public class BugReportResponsiblePO extends BaseEntity {

    @TableField("report_id")
    private Long reportId;

    @TableField("user_id")
    private Long userId;
}
