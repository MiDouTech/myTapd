package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工作流详情输出
 */
@Data
public class WorkflowDetailOutput implements Serializable {

    private Long id;
    private String name;
    private String mode;
    private String modeLabel;
    private String description;
    private Integer isBuiltin;
    private Integer isActive;
    private List<StateItem> states;
    private List<TransitionItem> transitions;
    private Date createTime;
    private Date updateTime;

    @Data
    public static class StateItem implements Serializable {
        private String code;
        private String name;
        private String type;
        private String slaAction;
    }

    @Data
    public static class TransitionItem implements Serializable {
        private String from;
        private String fromName;
        private String to;
        private String toName;
        private String name;
        private List<String> allowedRoles;
    }
}
