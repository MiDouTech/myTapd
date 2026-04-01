package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 告警规则映射 - 创建请求
 */
@Data
public class AlertRuleMappingCreateInput implements Serializable {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 200, message = "规则名称长度不能超过200个字符")
    private String ruleName;

    @NotBlank(message = "匹配模式不能为空")
    private String matchMode;

    @NotNull(message = "工单分类不能为空")
    private Long categoryId;

    private String priorityP1;

    private String priorityP2;

    private String priorityP3;

    private Long assigneeId;

    private Integer dedupWindowMinutes;

    private Boolean enabled;
}
