package com.miduo.cloud.ticket.application.user;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.entity.dto.user.CurrentUserOutput;
import com.miduo.cloud.ticket.entity.dto.user.EmployeeDetailOutput;
import com.miduo.cloud.ticket.entity.dto.user.EmployeePageInput;
import com.miduo.cloud.ticket.entity.dto.user.EmployeePageOutput;
import com.miduo.cloud.ticket.entity.dto.user.UserListInput;
import com.miduo.cloud.ticket.entity.dto.user.UserListOutput;
import org.springframework.stereotype.Service;

import java.util.*;
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
        List<Long> userIds = users.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<String>> roleCodeMap = userRepository.findRoleCodesByUserIds(userIds);

        Map<Long, String> finalDeptNameMap = deptNameMap;
        Map<Long, List<String>> finalRoleCodeMap = roleCodeMap;
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
            output.setGender(user.getGender());
            output.setAvatarUrl(user.getAvatarUrl());
            output.setAccountStatus(user.getAccountStatus());
            output.setRoleCodes(finalRoleCodeMap.getOrDefault(user.getId(), Collections.emptyList()));
            output.setCreateTime(user.getCreateTime());
            return output;
        }).collect(Collectors.toList());
    }

    /**
     * 员工分页查询
     */
    public PageOutput<EmployeePageOutput> pageEmployees(EmployeePageInput input) {
        if (input == null) {
            input = new EmployeePageInput();
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return PageOutput.empty(input.getPageNum(), input.getPageSize());
        }

        if (input.getDepartmentId() != null) {
            Long targetDeptId = input.getDepartmentId();
            users = users.stream()
                    .filter(u -> targetDeptId.equals(u.getDepartmentId()))
                    .collect(Collectors.toList());
        }

        if (input.getKeyword() != null && !input.getKeyword().trim().isEmpty()) {
            String keyword = input.getKeyword().trim().toLowerCase();
            users = users.stream()
                    .filter(u -> containsIgnoreCase(u.getName(), keyword)
                            || containsIgnoreCase(u.getEmployeeNo(), keyword))
                    .collect(Collectors.toList());
        }

        if (input.getAccountStatus() != null) {
            Integer targetStatus = input.getAccountStatus();
            users = users.stream()
                    .filter(u -> targetStatus.equals(u.getAccountStatus()))
                    .collect(Collectors.toList());
        }
        if (input.getGender() != null) {
            Integer targetGender = input.getGender();
            users = users.stream()
                    .filter(u -> targetGender.equals(u.getGender()))
                    .collect(Collectors.toList());
        }
        if (input.getSyncStatus() != null) {
            Integer targetSyncStatus = input.getSyncStatus();
            users = users.stream()
                    .filter(u -> targetSyncStatus.equals(u.getSyncStatus()))
                    .collect(Collectors.toList());
        }

        Map<Long, String> deptNameMap = buildDepartmentNameMap(users);
        int total = users.size();
        int startIndex = (input.getPageNum() - 1) * input.getPageSize();
        if (startIndex >= total) {
            return PageOutput.empty(input.getPageNum(), input.getPageSize());
        }
        int endIndex = Math.min(startIndex + input.getPageSize(), total);
        List<User> pageUsers = users.subList(startIndex, endIndex);

        List<EmployeePageOutput> records = pageUsers.stream()
                .map(user -> toEmployeePageOutput(user, deptNameMap))
                .collect(Collectors.toList());
        return PageOutput.of(records, total, input.getPageNum(), input.getPageSize());
    }

    /**
     * 员工详情
     */
    public EmployeeDetailOutput getEmployeeDetail(Long id) {
        User user = userRepository.findById(id);
        if (user == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "员工不存在");
        }

        String departmentName = null;
        if (user.getDepartmentId() != null) {
            Department department = departmentRepository.findById(user.getDepartmentId());
            departmentName = department == null ? null : department.getName();
        }

        EmployeeDetailOutput output = new EmployeeDetailOutput();
        output.setId(user.getId());
        output.setName(user.getName());
        output.setEmployeeNo(user.getEmployeeNo());
        output.setDepartmentId(user.getDepartmentId());
        output.setDepartmentName(departmentName);
        output.setEmailMasked(maskEmail(user.getEmail()));
        output.setPhoneMasked(maskPhone(user.getPhone()));
        output.setPosition(user.getPosition());
        output.setGender(user.getGender());
        output.setGenderName(mapGenderName(user.getGender()));
        output.setAvatarUrl(user.getAvatarUrl());
        output.setWecomUseridMasked(maskWecomUserid(user.getWecomUserid()));
        output.setAccountStatus(user.getAccountStatus());
        output.setAccountStatusName(mapAccountStatusName(user.getAccountStatus()));
        output.setSyncStatus(user.getSyncStatus());
        output.setSyncStatusName(mapSyncStatusName(user.getSyncStatus()));
        output.setSyncTime(user.getSyncTime());
        output.setRoleCodes(userRepository.findRoleCodes(user.getId()));
        output.setCreateTime(user.getCreateTime());
        return output;
    }

    private EmployeePageOutput toEmployeePageOutput(User user, Map<Long, String> deptNameMap) {
        EmployeePageOutput output = new EmployeePageOutput();
        output.setId(user.getId());
        output.setName(user.getName());
        output.setEmployeeNo(user.getEmployeeNo());
        output.setDepartmentId(user.getDepartmentId());
        output.setDepartmentName(user.getDepartmentId() == null ? null : deptNameMap.get(user.getDepartmentId()));
        output.setEmailMasked(maskEmail(user.getEmail()));
        output.setPhoneMasked(maskPhone(user.getPhone()));
        output.setPosition(user.getPosition());
        output.setGender(user.getGender());
        output.setGenderName(mapGenderName(user.getGender()));
        output.setAvatarUrl(user.getAvatarUrl());
        output.setWecomUseridMasked(maskWecomUserid(user.getWecomUserid()));
        output.setAccountStatus(user.getAccountStatus());
        output.setAccountStatusName(mapAccountStatusName(user.getAccountStatus()));
        output.setSyncStatus(user.getSyncStatus());
        output.setSyncStatusName(mapSyncStatusName(user.getSyncStatus()));
        output.setSyncTime(user.getSyncTime());
        output.setCreateTime(user.getCreateTime());
        return output;
    }

    private Map<Long, String> buildDepartmentNameMap(List<User> users) {
        List<Long> deptIds = users.stream()
                .map(User::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return departmentRepository.findAll().stream()
                .filter(d -> deptIds.contains(d.getId()))
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String mapAccountStatusName(Integer accountStatus) {
        if (accountStatus == null) {
            return "未知";
        }
        switch (accountStatus) {
            case 1:
                return "在职";
            case 2:
                return "停用";
            case 4:
                return "离职";
            default:
                return "未知";
        }
    }

    private String mapGenderName(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 1:
                return "男";
            case 2:
                return "女";
            default:
                return "未知";
        }
    }

    private String mapSyncStatusName(Integer syncStatus) {
        if (syncStatus == null) {
            return "未知";
        }
        switch (syncStatus) {
            case 0:
                return "未同步";
            case 1:
                return "成功";
            case 2:
                return "失败/失效";
            default:
                return "未知";
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        String value = phone.trim();
        if (value.length() <= 7) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "";
        }
        String value = email.trim();
        int atIndex = value.indexOf("@");
        if (atIndex <= 1) {
            return "****";
        }
        return value.substring(0, 1) + "****" + value.substring(atIndex);
    }

    private String maskWecomUserid(String wecomUserid) {
        if (wecomUserid == null || wecomUserid.trim().isEmpty()) {
            return "";
        }
        String value = wecomUserid.trim();
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
