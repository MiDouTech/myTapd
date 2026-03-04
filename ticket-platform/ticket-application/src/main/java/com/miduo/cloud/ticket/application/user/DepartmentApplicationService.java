package com.miduo.cloud.ticket.application.user;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.entity.dto.user.DepartmentTreeOutput;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 部门应用服务
 */
@Service
public class DepartmentApplicationService extends BaseApplicationService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentApplicationService(DepartmentRepository departmentRepository,
                                        UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取部门树
     */
    public List<DepartmentTreeOutput> getDepartmentTree() {
        List<Department> allDepartments = departmentRepository.findAll();
        if (allDepartments == null || allDepartments.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<Department>> childrenMap = allDepartments.stream()
                .filter(d -> d.getParentId() != null)
                .collect(Collectors.groupingBy(Department::getParentId));

        Map<Long, Integer> directUserCountMap = buildDirectUserCountMap();
        List<Department> roots = allDepartments.stream()
                .filter(d -> d.getParentId() == null)
                .collect(Collectors.toList());

        return roots.stream()
                .map(root -> buildTree(root, childrenMap, directUserCountMap))
                .collect(Collectors.toList());
    }

    private DepartmentTreeOutput buildTree(Department dept,
                                           Map<Long, List<Department>> childrenMap,
                                           Map<Long, Integer> directUserCountMap) {
        DepartmentTreeOutput output = new DepartmentTreeOutput();
        output.setId(dept.getId());
        output.setName(dept.getName());
        output.setParentId(dept.getParentId());
        output.setWecomDeptId(dept.getWecomDeptId());
        output.setSortOrder(dept.getSortOrder());
        output.setDeptStatus(dept.getDeptStatus() == null ? 1 : dept.getDeptStatus());
        output.setSyncStatus(dept.getSyncStatus() == null ? 0 : dept.getSyncStatus());
        output.setSyncTime(dept.getSyncTime());
        output.setDirectUserCount(directUserCountMap.getOrDefault(dept.getId(), 0));
        output.setTotalUserCount(output.getDirectUserCount());

        List<Department> children = childrenMap.get(dept.getId());
        if (children != null && !children.isEmpty()) {
            List<DepartmentTreeOutput> childNodes = children.stream()
                    .map(child -> buildTree(child, childrenMap, directUserCountMap))
                    .collect(Collectors.toList());
            output.setChildren(childNodes);
            output.setTotalUserCount(sumTotalUserCount(output.getDirectUserCount(), childNodes));
        } else {
            output.setChildren(new ArrayList<>());
        }

        return output;
    }

    private int sumTotalUserCount(Integer currentCount, List<DepartmentTreeOutput> children) {
        int total = currentCount == null ? 0 : currentCount;
        if (children == null || children.isEmpty()) {
            return total;
        }
        for (DepartmentTreeOutput child : children) {
            total += child.getTotalUserCount() == null ? 0 : child.getTotalUserCount();
        }
        return total;
    }

    private Map<Long, Integer> buildDirectUserCountMap() {
        List<User> activeUsers = userRepository.findAllActive();
        if (activeUsers == null || activeUsers.isEmpty()) {
            return Collections.emptyMap();
        }
        return activeUsers.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(deptId -> deptId, deptId -> 1, Integer::sum));
    }
}
