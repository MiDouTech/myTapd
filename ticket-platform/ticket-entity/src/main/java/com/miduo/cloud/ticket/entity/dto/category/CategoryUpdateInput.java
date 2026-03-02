package com.miduo.cloud.ticket.entity.dto.category;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class CategoryUpdateInput implements Serializable {

    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    private String name;

    private Long templateId;

    private Long workflowId;

    private Long slaPolicyId;

    private Long defaultGroupId;

    private Integer sortOrder;

    private Integer isActive;
}
