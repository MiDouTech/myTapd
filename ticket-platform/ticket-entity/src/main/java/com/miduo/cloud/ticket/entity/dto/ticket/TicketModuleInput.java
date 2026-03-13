package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 工单模块创建入参
 */
@Data
public class TicketModuleInput implements Serializable {

    @NotBlank(message = "模块名称不能为空")
    @Size(max = 100, message = "模块名称长度不能超过100个字符")
    private String name;
}
