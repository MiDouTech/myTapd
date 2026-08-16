package com.miduo.cloud.ticket.entity.dto.customfield;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class CustomFieldOptionInput implements Serializable {

    @NotBlank(message = "选项名称不能为空")
    @Size(max = 100, message = "选项名称长度不能超过100个字符")
    private String label;

    @NotBlank(message = "选项值不能为空")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{0,99}$", message = "选项值只能包含字母、数字、下划线和短横线")
    private String value;

    private Integer sortOrder;

    private Integer enabled;
}
