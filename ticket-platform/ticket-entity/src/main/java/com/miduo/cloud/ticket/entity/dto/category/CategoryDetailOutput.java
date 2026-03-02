package com.miduo.cloud.ticket.entity.dto.category;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CategoryDetailOutput implements Serializable {

    private Long id;

    private String name;

    private Long parentId;

    private String parentName;

    private Integer level;

    private String path;

    private String fullPathName;

    private Long templateId;

    private String templateName;

    private Long workflowId;

    private String workflowName;

    private Long slaPolicyId;

    private String slaPolicyName;

    private Long defaultGroupId;

    private String defaultGroupName;

    private Integer sortOrder;

    private Integer isActive;

    private Date createTime;

    private Date updateTime;
}
