package com.miduo.cloud.ticket.entity.dto.category;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class CategoryCreateInput implements Serializable {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    private String name;

    private Long parentId;

    @NotNull(message = "分类层级不能为空")
    private Integer level;

    private Long templateId;

    private Long workflowId;

    private Long slaPolicyId;

    private Long defaultGroupId;

    private Integer sortOrder;

    @Size(max = 500, message = "备注描述长度不能超过500个字符")
    private String remark;

    private String nlMatchKeywords;
}
