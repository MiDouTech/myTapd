package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警建单策略配置 - 输出
 */
@Data
public class AlertCreatePolicyOutput implements Serializable {

    /**
     * 是否仅命中映射规则时创建工单
     */
    private Boolean mappedOnlyCreate;
}
