package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 告警建单策略配置 - 更新输入
 */
@Data
public class AlertCreatePolicyUpdateInput implements Serializable {

    /**
     * 是否仅命中映射规则时创建工单
     */
    @NotNull(message = "建单策略开关不能为空")
    private Boolean mappedOnlyCreate;
}
