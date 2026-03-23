package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * SLA计时器持久化对象
 */
@Data
@TableName("sla_timer")
public class SlaTimerPO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("ticket_id")
    private Long ticketId;

    @TableField("sla_policy_id")
    private Long slaPolicyId;

    @TableField("timer_type")
    private String timerType;

    @TableField("status")
    private String status;

    @TableField("threshold_minutes")
    private Integer thresholdMinutes;

    @TableField("elapsed_minutes")
    private Integer elapsedMinutes;

    @TableField("base_elapsed_minutes")
    private Integer baseElapsedMinutes;

    @TableField("start_at")
    private Date startAt;

    @TableField("pause_at")
    private Date pauseAt;

    @TableField("deadline")
    private Date deadline;

    @TableField("breached_at")
    private Date breachedAt;

    @TableField("completed_at")
    private Date completedAt;

    @TableField("is_warned")
    private Integer isWarned;

    @TableField("is_breached")
    private Integer isBreached;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
