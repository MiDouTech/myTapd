package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 退回请求
 */
@Data
public class ReturnInput implements Serializable {

    @NotBlank(message = "退回原因不能为空")
    private String reason;
}
