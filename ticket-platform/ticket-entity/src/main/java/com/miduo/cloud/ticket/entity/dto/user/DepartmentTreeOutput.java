package com.miduo.cloud.ticket.entity.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部门树节点输出
 */
@Data
public class DepartmentTreeOutput implements Serializable {

    private Long id;
    private String name;
    private Long parentId;
    private Long wecomDeptId;
    private Integer sortOrder;
    private List<DepartmentTreeOutput> children;
}
