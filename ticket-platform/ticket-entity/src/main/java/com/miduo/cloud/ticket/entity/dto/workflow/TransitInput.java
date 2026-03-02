package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 状态流转请求
 */
@Data
public class TransitInput implements Serializable {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    private String remark;
}
