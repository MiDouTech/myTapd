package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 转派请求
 */
@Data
public class TransferInput implements Serializable {

    @NotNull(message = "目标处理人ID不能为空")
    private Long targetUserId;

    @NotBlank(message = "转派原因不能为空")
    private String reason;
}
