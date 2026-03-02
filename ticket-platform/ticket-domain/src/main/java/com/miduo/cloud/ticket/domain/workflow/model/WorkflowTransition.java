package com.miduo.cloud.ticket.domain.workflow.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工作流流转规则值对象
 */
@Data
public class WorkflowTransition implements Serializable {

    private String from;
    private String to;
    private String name;
    private List<String> allowedRoles;
}
