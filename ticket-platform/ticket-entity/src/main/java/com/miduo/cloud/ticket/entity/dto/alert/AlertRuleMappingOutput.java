package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 告警规则映射 - 输出
 */
@Data
public class AlertRuleMappingOutput implements Serializable {

    private Long id;

    private String ruleName;

    private String matchMode;

    private Long categoryId;

    private String categoryName;

    private String priorityP1;

    private String priorityP2;

    private String priorityP3;

    private Long assigneeId;

    private String assigneeName;

    private Integer dedupWindowMinutes;

    private Boolean enabled;

    private Date createTime;

    private Date updateTime;
}
