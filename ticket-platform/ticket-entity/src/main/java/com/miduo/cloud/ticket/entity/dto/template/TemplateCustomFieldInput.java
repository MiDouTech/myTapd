package com.miduo.cloud.ticket.entity.dto.template;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class TemplateCustomFieldInput implements Serializable {

    @NotNull(message = "自定义字段ID不能为空")
    private Long customFieldId;

    private Integer sortOrder;
    private Integer isEnabled;
    private Integer isRequired;

    @Size(max = 200, message = "占位提示长度不能超过200个字符")
    private String placeholderOverride;

    private String defaultValueOverride;
}
