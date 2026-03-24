package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 工作流更新请求（自定义工作流；内置工作流禁止修改）
 */
@Data
public class WorkflowUpdateInput implements Serializable {

    @NotBlank(message = "工作流名称不能为空")
    private String name;

    @NotBlank(message = "工作流模式不能为空")
    private String mode;

    private String description;

    @NotNull(message = "启用状态不能为空")
    private Integer isActive;

    @NotEmpty(message = "状态定义不能为空")
    @Valid
    private List<StateItemInput> states;

    @NotEmpty(message = "流转规则不能为空")
    @Valid
    private List<TransitionItemInput> transitions;

    @Data
    public static class StateItemInput implements Serializable {

        @NotBlank(message = "状态编码不能为空")
        private String code;

        @NotBlank(message = "状态名称不能为空")
        private String name;

        @NotBlank(message = "状态类型不能为空")
        private String type;

        private String slaAction;

        private Integer order;
    }

    @Data
    public static class TransitionItemInput implements Serializable {

        private String id;

        @NotBlank(message = "流转起始状态不能为空")
        private String from;

        @NotBlank(message = "流转目标状态不能为空")
        private String to;

        @NotBlank(message = "流转名称不能为空")
        private String name;

        private List<String> allowedRoles;

        private Boolean requireRemark;

        private Boolean allowTransfer;

        private Boolean isReturn;
    }
}
