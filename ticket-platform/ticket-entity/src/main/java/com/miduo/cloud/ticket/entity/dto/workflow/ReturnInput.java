package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 退回请求
 * 退回操作由工作流中 isReturn=true 的流转驱动
 */
@Data
public class ReturnInput implements Serializable {

    /**
     * 退回目标状态码（可选）
     * 若当前状态有多个退回路径时，通过此字段指定目标状态
     * 不填时使用工作流中第一个 isReturn=true 的流转
     */
    private String targetStatus;

    /**
     * 退回原因（必填）
     */
    @NotBlank(message = "退回原因不能为空")
    private String reason;
}
