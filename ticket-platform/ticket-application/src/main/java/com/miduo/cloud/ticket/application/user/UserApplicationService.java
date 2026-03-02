package com.miduo.cloud.ticket.application.user;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.entity.dto.user.CurrentUserOutput;
import com.miduo.cloud.ticket.entity.dto.user.UserListInput;
import com.miduo.cloud.ticket.entity.dto.user.UserListOutput;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户应用服务
 */
@Service
public class UserApplicationService extends BaseApplicationService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public UserApplicationService(UserRepository userRepository,
                                  DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * 获取当前用户信息
     */
    public CurrentUserOutput getCurrentUser(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        List<String> roleCodes = userRepository.findRoleCodes(userId);

        String deptName = null;
        if (user.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(user.getDepartmentId());
            if (dept != null) {
                deptName = dept.getName();
            }
        }

        CurrentUserOutput output = new CurrentUserOutput();
        output.setId(user.getId());
        output.setName(user.getName());
        output.setEmployeeNo(user.getEmployeeNo());
        output.setDepartmentId(user.getDepartmentId());
        output.setDepartmentName(deptName);
        output.setEmail(user.getEmail());
        output.setPhone(user.getPhone());
        output.setPosition(user.getPosition());
        output.setAvatarUrl(user.getAvatarUrl());
        output.setWecomUserid(user.getWecomUserid());
        output.setAccountStatus(user.getAccountStatus());
        output.setRoleCodes(roleCodes);
        output.setCreateTime(user.getCreateTime());

        return output;
    }

    /**
     * 查询用户列表
     */
    public List<UserListOutput> getUserList(UserListInput input) {
        List<User> users;

        if (input != null && input.getDepartmentId() != null) {
            users = userRepository.findByDepartmentId(input.getDepartmentId());
        } else {
            users = userRepository.findAllActive();
        }

        if (input != null && input.getKeyword() != null && !input.getKeyword().trim().isEmpty()) {
            String keyword = input.getKeyword().trim().toLowerCase();
            users = users.stream()
                    .filter(u -> {
                        boolean match = false;
                        if (u.getName() != null) {
                            match = u.getName().toLowerCase().contains(keyword);
                        }
                        if (!match && u.getEmployeeNo() != null) {
                            match = u.getEmployeeNo().toLowerCase().contains(keyword);
                        }
                        return match;
                    })
                    .collect(Collectors.toList());
        }

        if (input != null && input.getAccountStatus() != null) {
            users = users.stream()
                    .filter(u -> input.getAccountStatus().equals(u.getAccountStatus()))
                    .collect(Collectors.toList());
        }

        List<Long> deptIds = users.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<Department> departments = departmentRepository.findAll();
            deptNameMap = departments.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
        }

        Map<Long, String> finalDeptNameMap = deptNameMap;
        return users.stream().map(user -> {
            UserListOutput output = new UserListOutput();
            output.setId(user.getId());
            output.setName(user.getName());
            output.setEmployeeNo(user.getEmployeeNo());
            output.setDepartmentId(user.getDepartmentId());
            output.setDepartmentName(user.getDepartmentId() != null ? finalDeptNameMap.get(user.getDepartmentId()) : null);
            output.setEmail(user.getEmail());
            output.setPhone(user.getPhone());
            output.setPosition(user.getPosition());
            output.setAvatarUrl(user.getAvatarUrl());
            output.setAccountStatus(user.getAccountStatus());
            output.setRoleCodes(userRepository.findRoleCodes(user.getId()));
            output.setCreateTime(user.getCreateTime());
            return output;
        }).collect(Collectors.toList());
    }
}
