package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Bug简报工单关联PO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bug_report_ticket")
public class BugReportTicketPO extends BaseEntity {

    @TableField("report_id")
    private Long reportId;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("is_auto_created")
    private Integer isAutoCreated;
}
