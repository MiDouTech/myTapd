package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 审批操作请求体
 * 接口编号：API000520（同意/驳回/转交 公用一个接口，通过 actionType 区分）
 */
@Data
public class ApprovalActionInput implements Serializable {

    /**
     * 审批任务ID
     */
    @NotNull(message = "taskId不能为空")
    private Long taskId;

    /**
     * 操作类型：approve（同意）/ reject（驳回）/ transfer（转交）
     */
    @NotBlank(message = "操作类型不能为空")
    private String actionType;

    /**
     * 审批意见（驳回时建议必填，前端控制）
     */
    private String remark;

    /**
     * 转交目标用户ID（actionType=transfer 时必填）
     */
    private Long targetAssigneeId;
}
