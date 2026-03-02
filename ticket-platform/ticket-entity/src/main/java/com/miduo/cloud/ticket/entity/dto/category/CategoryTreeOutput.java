package com.miduo.cloud.ticket.entity.dto.category;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CategoryTreeOutput implements Serializable {

    private Long id;

    private String name;

    private Long parentId;

    private Integer level;

    private String path;

    private Long templateId;

    private Long workflowId;

    private Long slaPolicyId;

    private Long defaultGroupId;

    private Integer sortOrder;

    private Integer isActive;

    private List<CategoryTreeOutput> children;
}
