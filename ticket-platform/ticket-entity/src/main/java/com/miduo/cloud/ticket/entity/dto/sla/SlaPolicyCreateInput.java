package com.miduo.cloud.ticket.entity.dto.sla;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * SLA策略创建输入DTO
 */
@Data
public class SlaPolicyCreateInput implements Serializable {

    @NotBlank(message = "策略名称不能为空")
    private String name;

    @NotBlank(message = "优先级不能为空")
    private String priority;

    @NotNull(message = "首次响应时限不能为空")
    @Min(value = 1, message = "首次响应时限最少1分钟")
    private Integer responseTime;

    @NotNull(message = "解决时限不能为空")
    @Min(value = 1, message = "解决时限最少1分钟")
    private Integer resolveTime;

    @Min(value = 1, message = "预警百分比阈值最小为1")
    @Max(value = 100, message = "预警百分比阈值最大为100")
    private Integer warningPct;

    @Min(value = 1, message = "告警百分比阈值最小为1")
    @Max(value = 100, message = "告警百分比阈值最大为100")
    private Integer criticalPct;

    private String description;
}
