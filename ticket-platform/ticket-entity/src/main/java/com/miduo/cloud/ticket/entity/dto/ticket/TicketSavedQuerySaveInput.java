package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Data
public class TicketSavedQuerySaveInput {

    @NotBlank(message = "查询名称不能为空")
    @Size(max = 30, message = "查询名称不能超过30个字符")
    private String name;

    @NotNull(message = "查询配置不能为空")
    private Map<String, Object> queryConfig;

    @NotEmpty(message = "请至少选择一个导出字段")
    @Size(max = 50, message = "导出字段不能超过50个")
    private List<String> exportFields;
}
