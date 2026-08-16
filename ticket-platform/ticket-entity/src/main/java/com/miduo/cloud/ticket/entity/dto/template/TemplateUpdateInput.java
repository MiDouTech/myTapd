package com.miduo.cloud.ticket.entity.dto.template;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Data
public class TemplateUpdateInput implements Serializable {

    @NotNull(message = "模板ID不能为空")
    private Long id;

    @Size(min = 1, max = 100, message = "模板名称长度须为1-100个字符")
    private String name;

    @Size(max = 500, message = "模板描述长度不能超过500个字符")
    private String description;

    private Integer isActive;

    @Valid
    private List<TemplateCustomFieldInput> customFields;
}
