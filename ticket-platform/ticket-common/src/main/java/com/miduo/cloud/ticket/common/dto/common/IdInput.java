package com.miduo.cloud.ticket.common.dto.common;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * ID 请求参数
 */
@Data
public class IdInput implements Serializable {

    @NotNull(message = "ID不能为空")
    private Long id;
}
