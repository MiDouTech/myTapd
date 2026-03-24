package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 工作流列表输出
 */
@Data
public class WorkflowListOutput implements Serializable {

    private Long id;
    private String name;
    private String mode;
    private String modeLabel;
    private String description;
    private Integer isBuiltin;
    private Integer isActive;
    private Integer stateCount;
    private Integer transitionCount;
    private Date createTime;
    private Date updateTime;
}
