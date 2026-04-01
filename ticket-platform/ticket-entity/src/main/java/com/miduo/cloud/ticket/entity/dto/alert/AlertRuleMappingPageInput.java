package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警规则映射 - 分页查询请求
 */
@Data
public class AlertRuleMappingPageInput implements Serializable {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String ruleName;

    private Boolean enabled;
}
