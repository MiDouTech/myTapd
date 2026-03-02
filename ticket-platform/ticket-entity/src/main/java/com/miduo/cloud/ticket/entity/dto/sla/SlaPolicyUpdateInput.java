package com.miduo.cloud.ticket.entity.dto.sla;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * SLA策略更新输入DTO
 */
@Data
public class SlaPolicyUpdateInput implements Serializable {

    @NotNull(message = "策略ID不能为空")
    private Long id;

    private String name;

    private String priority;

    @Min(value = 1, message = "首次响应时限最少1分钟")
    private Integer responseTime;

    @Min(value = 1, message = "解决时限最少1分钟")
    private Integer resolveTime;

    @Min(value = 1, message = "预警百分比阈值最小为1")
    @Max(value = 100, message = "预警百分比阈值最大为100")
    private Integer warningPct;

    @Min(value = 1, message = "告警百分比阈值最小为1")
    @Max(value = 100, message = "告警百分比阈值最大为100")
    private Integer criticalPct;

    private String description;

    private Integer isActive;
}
