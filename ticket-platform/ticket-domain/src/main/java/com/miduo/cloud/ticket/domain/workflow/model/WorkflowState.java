package com.miduo.cloud.ticket.domain.workflow.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 工作流状态值对象
 */
@Data
public class WorkflowState implements Serializable {

    private String code;
    private String name;
    private String type;
    private String slaAction;
}
