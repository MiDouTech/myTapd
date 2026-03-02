package com.miduo.cloud.ticket.domain.user.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 部门领域模型
 */
@Data
public class Department implements Serializable {

    private Long id;
    private String name;
    private Long parentId;
    private Long wecomDeptId;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;

    private List<Department> children;
}
