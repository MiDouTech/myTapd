package com.miduo.cloud.ticket.entity.dto.alert;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 告警规则映射 - 更新请求
 */
@Data
public class AlertRuleMappingUpdateInput implements Serializable {

    @NotNull(message = "ID不能为空")
    private Long id;

    @Size(max = 200, message = "规则名称长度不能超过200个字符")
    private String ruleName;

    private String matchMode;

    private Long categoryId;

    private String priorityP1;

    private String priorityP2;

    private String priorityP3;

    private Long assigneeId;

    private Integer dedupWindowMinutes;

    private Boolean enabled;
}
