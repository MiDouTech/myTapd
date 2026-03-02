package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 工单节点耗时统计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_node_duration")
public class TicketNodeDurationPO extends BaseEntity {

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("node_name")
    private String nodeName;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("assignee_role")
    private String assigneeRole;

    @TableField("arrive_at")
    private Date arriveAt;

    @TableField("first_read_at")
    private Date firstReadAt;

    @TableField("start_process_at")
    private Date startProcessAt;

    @TableField("leave_at")
    private Date leaveAt;

    @TableField("wait_duration_sec")
    private Long waitDurationSec;

    @TableField("process_duration_sec")
    private Long processDurationSec;

    @TableField("total_duration_sec")
    private Long totalDurationSec;
}
