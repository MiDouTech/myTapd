package com.miduo.cloud.ticket.entity.dto.customfield;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Data
public class CustomFieldUpdateInput implements Serializable {

    @NotNull(message = "字段ID不能为空")
    private Long id;

    @Size(min = 1, max = 100, message = "字段标题长度须为1-100个字符")
    private String fieldTitle;

    @Size(max = 500, message = "字段说明长度不能超过500个字符")
    private String description;

    @Size(max = 200, message = "占位提示长度不能超过200个字符")
    private String placeholder;

    private Integer defaultRequired;

    private String defaultValue;

    private Integer maxLength;

    @Valid
    private List<CustomFieldOptionInput> options;

    private Integer isActive;
}
