package com.miduo.cloud.ticket.entity.dto.system;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 基础参数配置更新输入对象
 */
@Data
public class BasicSettingsUpdateInput implements Serializable {

    @NotBlank(message = "系统名称不能为空")
    private String systemName;

    @NotBlank(message = "时区不能为空")
    private String timezone;

    @NotBlank(message = "工作开始时间不能为空")
    private String workTimeStart;

    @NotBlank(message = "工作结束时间不能为空")
    private String workTimeEnd;

    @NotNull(message = "默认分页条数不能为空")
    private Integer defaultPageSize;
}
