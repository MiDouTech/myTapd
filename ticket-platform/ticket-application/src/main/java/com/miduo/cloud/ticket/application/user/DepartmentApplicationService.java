package com.miduo.cloud.ticket.application.user;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.entity.dto.user.DepartmentTreeOutput;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门应用服务
 */
@Service
public class DepartmentApplicationService extends BaseApplicationService {

    private final DepartmentRepository departmentRepository;

    public DepartmentApplicationService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    /**
     * 获取部门树
     */
    public List<DepartmentTreeOutput> getDepartmentTree() {
        List<Department> allDepartments = departmentRepository.findAll();

        Map<Long, List<Department>> childrenMap = allDepartments.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(Department::getParentId));

        List<Department> roots = allDepartments.stream()
                .filter(d -> d.getParentId() == null)
                .collect(Collectors.toList());

        return roots.stream()
                .map(root -> buildTree(root, childrenMap))
                .collect(Collectors.toList());
    }

    private DepartmentTreeOutput buildTree(Department dept, Map<Long, List<Department>> childrenMap) {
        DepartmentTreeOutput output = new DepartmentTreeOutput();
        output.setId(dept.getId());
        output.setName(dept.getName());
        output.setParentId(dept.getParentId());
        output.setWecomDeptId(dept.getWecomDeptId());
        output.setSortOrder(dept.getSortOrder());

        List<Department> children = childrenMap.get(dept.getId());
        if (children != null && !children.isEmpty()) {
            output.setChildren(children.stream()
                    .map(child -> buildTree(child, childrenMap))
                    .collect(Collectors.toList()));
        } else {
            output.setChildren(new ArrayList<>());
        }

        return output;
    }
}
