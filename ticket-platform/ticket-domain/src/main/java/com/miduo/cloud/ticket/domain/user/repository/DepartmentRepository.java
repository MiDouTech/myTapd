package com.miduo.cloud.ticket.domain.user.repository;

import com.miduo.cloud.ticket.domain.user.model.Department;

import java.util.List;

/**
 * 部门仓储接口
 */
public interface DepartmentRepository {

    /**
     * 根据ID查询部门
     */
    Department findById(Long id);

    /**
     * 根据企微部门ID查询部门
     */
    Department findByWecomDeptId(Long wecomDeptId);

    /**
     * 查询所有部门
     */
    List<Department> findAll();

    /**
     * 保存部门（新增或更新）
     */
    Department save(Department department);

    /**
     * 批量保存部门
     */
    void batchSave(List<Department> departments);

    /**
     * 查询子部门
     */
    List<Department> findByParentId(Long parentId);
}
