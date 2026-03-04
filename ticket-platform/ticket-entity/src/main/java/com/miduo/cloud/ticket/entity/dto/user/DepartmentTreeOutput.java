package com.miduo.cloud.ticket.entity.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
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
    private Integer deptStatus;
    private Integer syncStatus;
    private Date syncTime;
    private Integer directUserCount;
    private Integer totalUserCount;
    private List<DepartmentTreeOutput> children;
}
