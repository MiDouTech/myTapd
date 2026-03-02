package com.miduo.cloud.ticket.entity.dto.sla;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * SLA策略输出DTO
 */
@Data
public class SlaPolicyOutput implements Serializable {

    private Long id;

    private String name;

    private String priority;

    private String priorityLabel;

    private Integer responseTime;

    private Integer resolveTime;

    private Integer warningPct;

    private Integer criticalPct;

    private String description;

    private Integer isActive;

    private Date createTime;

    private Date updateTime;
}
