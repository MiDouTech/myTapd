package com.miduo.cloud.ticket.infrastructure.persistence.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.DepartmentMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.DepartmentPO;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门仓储实现
 */
@Repository
public class DepartmentRepositoryImpl implements DepartmentRepository {

    private final DepartmentMapper departmentMapper;

    public DepartmentRepositoryImpl(DepartmentMapper departmentMapper) {
        this.departmentMapper = departmentMapper;
    }

    @Override
    public Department findById(Long id) {
        if (id == null) {
            return null;
        }
        DepartmentPO po = departmentMapper.selectById(id);
        return po != null ? convertToModel(po) : null;
    }

    @Override
    public Department findByWecomDeptId(Long wecomDeptId) {
        if (wecomDeptId == null) {
            return null;
        }
        LambdaQueryWrapper<DepartmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepartmentPO::getWecomDeptId, wecomDeptId);
        DepartmentPO po = departmentMapper.selectOne(wrapper);
        return po != null ? convertToModel(po) : null;
    }

    @Override
    public List<Department> findAll() {
        LambdaQueryWrapper<DepartmentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(DepartmentPO::getSortOrder);
        wrapper.orderByAsc(DepartmentPO::getId);
        List<DepartmentPO> list = departmentMapper.selectList(wrapper);
        return list.stream().map(this::convertToModel).collect(Collectors.toList());
    }

    @Override
    public Department save(Department department) {
        if (department == null) {
            return null;
        }
        DepartmentPO po = convertToPO(department);
        if (department.getId() != null) {
            departmentMapper.updateById(po);
        } else {
            departmentMapper.insert(po);
            department.setId(po.getId());
        }
        return department;
    }

    @Override
    public void batchSave(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return;
        }
        for (Department dept : departments) {
            save(dept);
        }
    }

    @Override
    public List<Department> findByParentId(Long parentId) {
        LambdaQueryWrapper<DepartmentPO> wrapper = new LambdaQueryWrapper<>();
        if (parentId == null) {
            wrapper.isNull(DepartmentPO::getParentId);
        } else {
            wrapper.eq(DepartmentPO::getParentId, parentId);
        }
        wrapper.orderByAsc(DepartmentPO::getSortOrder);
        wrapper.orderByAsc(DepartmentPO::getId);
        List<DepartmentPO> list = departmentMapper.selectList(wrapper);
        return list.stream().map(this::convertToModel).collect(Collectors.toList());
    }

    private Department convertToModel(DepartmentPO po) {
        Department dept = new Department();
        dept.setId(po.getId());
        dept.setName(po.getName());
        dept.setParentId(po.getParentId());
        dept.setWecomDeptId(po.getWecomDeptId());
        dept.setSortOrder(po.getSortOrder());
        dept.setCreateTime(po.getCreateTime());
        dept.setUpdateTime(po.getUpdateTime());
        dept.setChildren(new ArrayList<>());
        return dept;
    }

    private DepartmentPO convertToPO(Department dept) {
        DepartmentPO po = new DepartmentPO();
        po.setId(dept.getId());
        po.setName(dept.getName());
        po.setParentId(dept.getParentId());
        po.setWecomDeptId(dept.getWecomDeptId());
        po.setSortOrder(dept.getSortOrder());
        return po;
    }
}
