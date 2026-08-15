package com.miduo.cloud.ticket.entity.dto.customfield;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Data
public class CustomFieldCreateInput implements Serializable {

    @NotBlank(message = "字段标题不能为空")
    @Size(max = 100, message = "字段标题长度不能超过100个字符")
    private String fieldTitle;

    @NotBlank(message = "字段编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,99}$", message = "字段编码须以小写字母开头，且只能包含小写字母、数字和下划线")
    private String fieldCode;

    @NotBlank(message = "字段类型不能为空")
    private String fieldType;

    @Size(max = 500, message = "字段说明长度不能超过500个字符")
    private String description;

    @Size(max = 200, message = "占位提示长度不能超过200个字符")
    private String placeholder;

    private Integer defaultRequired;

    private String defaultValue;

    private Integer maxLength;

    @Valid
    private List<CustomFieldOptionInput> options;
}
