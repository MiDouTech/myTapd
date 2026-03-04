package com.miduo.cloud.ticket.application.user;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企微通讯录同步服务
 * 全量同步部门和成员数据
 */
@Service
public class WecomSyncService extends BaseApplicationService {

    private static final Long ROOT_DEPARTMENT_ID = 1L;
    private static final Long DEFAULT_SUBMITTER_ROLE_ID = 4L;

    private final WecomClient wecomClient;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public WecomSyncService(WecomClient wecomClient,
                            DepartmentRepository departmentRepository,
                            UserRepository userRepository) {
        this.wecomClient = wecomClient;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    /**
     * 全量同步通讯录（部门 + 成员）
     */
    @Transactional
    public void syncAll() {
        syncAllWithResult();
    }

    /**
     * 全量同步通讯录并返回统计结果
     */
    @Transactional
    public SyncResult syncAllWithResult() {
        log.info("开始全量同步企微通讯录...");
        Date startTime = new Date();

        SyncPartResult deptResult = syncDepartments();
        SyncPartResult userResult = syncUsers();

        log.info("企微通讯录全量同步完成");
        Date endTime = new Date();

        SyncResult result = new SyncResult();
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setTotalCount(deptResult.getTotalCount() + userResult.getTotalCount());
        result.setSuccessCount(deptResult.getSuccessCount() + userResult.getSuccessCount());
        result.setFailCount(deptResult.getFailCount() + userResult.getFailCount());
        result.setErrorMessage(joinErrors(deptResult.getErrorMessage(), userResult.getErrorMessage()));
        result.setDepartmentCreatedCount(deptResult.getCreatedCount());
        result.setDepartmentUpdatedCount(deptResult.getUpdatedCount());
        result.setDepartmentDisabledCount(deptResult.getDisabledCount());
        result.setUserCreatedCount(userResult.getCreatedCount());
        result.setUserUpdatedCount(userResult.getUpdatedCount());
        result.setUserDisabledCount(userResult.getDisabledCount());
        return result;
    }

    /**
     * 同步部门
     */
    private SyncPartResult syncDepartments() {
        log.info("开始同步企微部门...");
        SyncPartResult result = new SyncPartResult();
        List<WecomClient.WecomDepartment> wecomDepts = wecomClient.getDepartmentList();
        if (wecomDepts == null) {
            wecomDepts = Collections.emptyList();
        }
        Date syncTime = new Date();

        Map<Long, Department> existingMap = departmentRepository.findAll().stream()
                .filter(d -> d.getWecomDeptId() != null)
                .collect(Collectors.toMap(Department::getWecomDeptId, Function.identity(), (a, b) -> a));
        Set<Long> incomingDeptIds = new HashSet<>();

        for (WecomClient.WecomDepartment wecomDept : wecomDepts) {
            if (wecomDept == null || wecomDept.getId() == null) {
                continue;
            }
            incomingDeptIds.add(wecomDept.getId());
            try {
                Department existing = existingMap.get(wecomDept.getId());
                if (existing == null) {
                    Department newDept = new Department();
                    newDept.setName(trimToNull(wecomDept.getName()));
                    newDept.setWecomDeptId(wecomDept.getId());
                    newDept.setSortOrder(defaultOrder(wecomDept.getOrder()));
                    newDept.setDeptStatus(1);
                    newDept.setSyncStatus(1);
                    newDept.setSyncTime(syncTime);
                    newDept.setLeaderWecomUserid(trimToNull(wecomDept.getPrimaryLeaderWecomUserid()));
                    Department saved = departmentRepository.save(newDept);
                    existingMap.put(wecomDept.getId(), saved);
                    result.setCreatedCount(result.getCreatedCount() + 1);
                    continue;
                }

                boolean changed = false;
                String latestName = trimToNull(wecomDept.getName());
                if (!Objects.equals(latestName, existing.getName())) {
                    existing.setName(latestName);
                    changed = true;
                }
                Integer latestOrder = defaultOrder(wecomDept.getOrder());
                if (!Objects.equals(latestOrder, existing.getSortOrder())) {
                    existing.setSortOrder(latestOrder);
                    changed = true;
                }
                String latestLeader = trimToNull(wecomDept.getPrimaryLeaderWecomUserid());
                if (!Objects.equals(latestLeader, existing.getLeaderWecomUserid())) {
                    existing.setLeaderWecomUserid(latestLeader);
                    changed = true;
                }
                if (!Integer.valueOf(1).equals(existing.getDeptStatus())) {
                    existing.setDeptStatus(1);
                    changed = true;
                }
                if (!Integer.valueOf(1).equals(existing.getSyncStatus())) {
                    existing.setSyncStatus(1);
                    changed = true;
                }

                if (changed) {
                    existing.setSyncTime(syncTime);
                    departmentRepository.save(existing);
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                }
            } catch (Exception ex) {
                result.setFailCount(result.getFailCount() + 1);
                result.appendError("部门[" + wecomDept.getId() + "]同步失败: " + ex.getMessage() + "; ");
            }
        }

        for (WecomClient.WecomDepartment wecomDept : wecomDepts) {
            if (wecomDept == null || wecomDept.getId() == null) {
                continue;
            }
            Department current = existingMap.get(wecomDept.getId());
            if (current == null) {
                continue;
            }
            try {
                Long targetParentId = resolveParentId(wecomDept.getParentId(), existingMap);
                if (!Objects.equals(targetParentId, current.getParentId())) {
                    current.setParentId(targetParentId);
                    current.setSyncTime(syncTime);
                    departmentRepository.save(current);
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                }
            } catch (Exception ex) {
                result.setFailCount(result.getFailCount() + 1);
                result.appendError("部门[" + wecomDept.getId() + "]父子关系同步失败: " + ex.getMessage() + "; ");
            }
        }

        List<Department> staleDepartments = new ArrayList<>();
        if (incomingDeptIds.isEmpty() && !existingMap.isEmpty()) {
            result.appendError("企微部门返回空列表，已跳过部门失活收敛; ");
        } else {
            staleDepartments = existingMap.values().stream()
                    .filter(d -> d.getWecomDeptId() != null && !incomingDeptIds.contains(d.getWecomDeptId()))
                    .collect(Collectors.toList());
            for (Department staleDept : staleDepartments) {
                try {
                    boolean changed = false;
                    if (!Integer.valueOf(0).equals(staleDept.getDeptStatus())) {
                        staleDept.setDeptStatus(0);
                        changed = true;
                    }
                    if (!Integer.valueOf(2).equals(staleDept.getSyncStatus())) {
                        staleDept.setSyncStatus(2);
                        changed = true;
                    }
                    if (changed) {
                        staleDept.setSyncTime(syncTime);
                        departmentRepository.save(staleDept);
                        result.setDisabledCount(result.getDisabledCount() + 1);
                    }
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.appendError("部门[" + staleDept.getWecomDeptId() + "]失活失败: " + ex.getMessage() + "; ");
                }
            }
        }

        result.setTotalCount(incomingDeptIds.size() + staleDepartments.size());
        result.setSuccessCount(Math.max(result.getTotalCount() - result.getFailCount(), 0));
        log.info("部门同步完成: 新增={}, 更新={}, 失活={}",
                result.getCreatedCount(), result.getUpdatedCount(), result.getDisabledCount());
        return result;
    }

    private Long resolveParentId(Long wecomParentId, Map<Long, Department> existingMap) {
        if (wecomParentId == null || wecomParentId <= 0) {
            return null;
        }
        Department parent = existingMap.get(wecomParentId);
        return parent != null ? parent.getId() : null;
    }

    /**
     * 同步用户（根部门递归拉取成员）
     */
    private SyncPartResult syncUsers() {
        log.info("开始同步企微用户...");
        SyncPartResult result = new SyncPartResult();

        List<Department> allDepts = departmentRepository.findAll();
        Map<Long, Department> wecomDeptMap = allDepts.stream()
                .filter(d -> d.getWecomDeptId() != null)
                .collect(Collectors.toMap(Department::getWecomDeptId, Function.identity(), (a, b) -> a));

        List<WecomClient.WecomUserDetail> wecomUsers;
        try {
            wecomUsers = wecomClient.getDepartmentUsers(ROOT_DEPARTMENT_ID, true);
        } catch (Exception ex) {
            result.setFailCount(result.getFailCount() + 1);
            result.appendError("获取企微成员列表失败: " + ex.getMessage() + "; ");
            result.setTotalCount(0);
            result.setSuccessCount(0);
            return result;
        }
        if (wecomUsers == null) {
            wecomUsers = Collections.emptyList();
        }

        Map<String, User> existingUserMap = userRepository.findAll().stream()
                .filter(u -> u.getWecomUserid() != null && !u.getWecomUserid().trim().isEmpty())
                .collect(Collectors.toMap(u -> u.getWecomUserid().trim(), Function.identity(), (a, b) -> a, HashMap::new));
        Set<String> incomingUserIds = new HashSet<>();
        Date syncTime = new Date();

        for (WecomClient.WecomUserDetail wecomUser : wecomUsers) {
            if (wecomUser == null || wecomUser.getUserId() == null || wecomUser.getUserId().trim().isEmpty()) {
                continue;
            }
            String wecomUserId = wecomUser.getUserId().trim();
            incomingUserIds.add(wecomUserId);
            try {
                User existing = existingUserMap.get(wecomUserId);
                if (existing == null) {
                    User newUser = new User();
                    newUser.setName(trimToNull(wecomUser.getName()));
                    newUser.setPhone(trimToNull(wecomUser.getMobile()));
                    newUser.setEmail(trimToNull(wecomUser.getEmail()));
                    newUser.setPosition(trimToNull(wecomUser.getPosition()));
                    newUser.setGender(normalizeGender(wecomUser.getGender()));
                    newUser.setAvatarUrl(trimToNull(wecomUser.getAvatar()));
                    newUser.setWecomUserid(wecomUserId);
                    newUser.setAccountStatus(mapWecomStatus(wecomUser.getStatus()));
                    newUser.setSyncStatus(1);
                    newUser.setSyncTime(syncTime);
                    newUser.setDepartmentId(resolveMainDepartment(wecomUser, wecomDeptMap));
                    User saved = userRepository.save(newUser);
                    userRepository.assignRole(saved.getId(), DEFAULT_SUBMITTER_ROLE_ID);
                    existingUserMap.put(wecomUserId, saved);
                    result.setCreatedCount(result.getCreatedCount() + 1);
                    continue;
                }

                boolean changed = false;
                String latestName = trimToNull(wecomUser.getName());
                if (!Objects.equals(latestName, existing.getName())) {
                    existing.setName(latestName);
                    changed = true;
                }
                String latestPhone = trimToNull(wecomUser.getMobile());
                if (!Objects.equals(latestPhone, existing.getPhone())) {
                    existing.setPhone(latestPhone);
                    changed = true;
                }
                String latestEmail = trimToNull(wecomUser.getEmail());
                if (!Objects.equals(latestEmail, existing.getEmail())) {
                    existing.setEmail(latestEmail);
                    changed = true;
                }
                String latestPosition = trimToNull(wecomUser.getPosition());
                if (!Objects.equals(latestPosition, existing.getPosition())) {
                    existing.setPosition(latestPosition);
                    changed = true;
                }
                Integer latestGender = normalizeGender(wecomUser.getGender());
                if (!Objects.equals(latestGender, existing.getGender())) {
                    existing.setGender(latestGender);
                    changed = true;
                }
                String latestAvatar = trimToNull(wecomUser.getAvatar());
                if (!Objects.equals(latestAvatar, existing.getAvatarUrl())) {
                    existing.setAvatarUrl(latestAvatar);
                    changed = true;
                }
                Long latestMainDeptId = resolveMainDepartment(wecomUser, wecomDeptMap);
                if (!Objects.equals(latestMainDeptId, existing.getDepartmentId())) {
                    existing.setDepartmentId(latestMainDeptId);
                    changed = true;
                }
                Integer latestAccountStatus = mapWecomStatus(wecomUser.getStatus());
                if (!Objects.equals(latestAccountStatus, existing.getAccountStatus())) {
                    existing.setAccountStatus(latestAccountStatus);
                    changed = true;
                }
                if (!Integer.valueOf(1).equals(existing.getSyncStatus())) {
                    existing.setSyncStatus(1);
                    changed = true;
                }

                if (changed) {
                    existing.setSyncTime(syncTime);
                    userRepository.save(existing);
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                }
            } catch (Exception ex) {
                result.setFailCount(result.getFailCount() + 1);
                result.appendError("用户[" + wecomUserId + "]同步失败: " + ex.getMessage() + "; ");
            }
        }

        List<User> staleUsers = new ArrayList<>();
        if (incomingUserIds.isEmpty() && !existingUserMap.isEmpty()) {
            result.appendError("企微成员返回空列表，已跳过用户失活收敛; ");
        } else {
            staleUsers = existingUserMap.values().stream()
                    .filter(u -> u.getWecomUserid() != null && !u.getWecomUserid().trim().isEmpty())
                    .filter(u -> !incomingUserIds.contains(u.getWecomUserid().trim()))
                    .collect(Collectors.toList());
            for (User staleUser : staleUsers) {
                try {
                    boolean changed = false;
                    if (!Integer.valueOf(4).equals(staleUser.getAccountStatus())) {
                        staleUser.setAccountStatus(4);
                        changed = true;
                    }
                    if (!Integer.valueOf(2).equals(staleUser.getSyncStatus())) {
                        staleUser.setSyncStatus(2);
                        changed = true;
                    }
                    if (changed) {
                        staleUser.setSyncTime(syncTime);
                        userRepository.save(staleUser);
                        result.setDisabledCount(result.getDisabledCount() + 1);
                    }
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.appendError("用户[" + staleUser.getWecomUserid() + "]失活失败: " + ex.getMessage() + "; ");
                }
            }
        }

        result.setTotalCount(incomingUserIds.size() + staleUsers.size());
        result.setSuccessCount(Math.max(result.getTotalCount() - result.getFailCount(), 0));
        log.info("用户同步完成: 新增={}, 更新={}, 失活={}",
                result.getCreatedCount(), result.getUpdatedCount(), result.getDisabledCount());
        return result;
    }

    private Long resolveMainDepartment(WecomClient.WecomUserDetail wecomUser, Map<Long, Department> wecomDeptMap) {
        if (wecomUser == null) {
            return null;
        }
        Long wecomDeptId = wecomUser.getMainDepartment();
        if (wecomDeptId == null && wecomUser.getDepartmentIds() != null && !wecomUser.getDepartmentIds().isEmpty()) {
            wecomDeptId = wecomUser.getDepartmentIds().get(0);
        }
        if (wecomDeptId == null) {
            return null;
        }
        Department dept = wecomDeptMap.get(wecomDeptId);
        return dept != null ? dept.getId() : null;
    }

    private Integer mapWecomStatus(Integer wecomStatus) {
        if (wecomStatus == null) {
            return 4;
        }
        switch (wecomStatus) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 4:
                return 4;
            case 5:
                return 4;
            default:
                return 4;
        }
    }

    private Integer normalizeGender(Integer gender) {
        if (gender == null) {
            return 0;
        }
        if (gender == 1 || gender == 2) {
            return gender;
        }
        return 0;
    }

    private Integer defaultOrder(Integer order) {
        return order == null ? 0 : order;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String joinErrors(String left, String right) {
        StringBuilder builder = new StringBuilder();
        if (left != null && !left.trim().isEmpty()) {
            builder.append(left.trim());
        }
        if (right != null && !right.trim().isEmpty()) {
            if (builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(right.trim());
        }
        return builder.toString();
    }

    @lombok.Data
    public static class SyncResult {
        private Date startTime;
        private Date endTime;
        private Integer totalCount;
        private Integer successCount;
        private Integer failCount;
        private String errorMessage;
        private Integer departmentCreatedCount;
        private Integer departmentUpdatedCount;
        private Integer departmentDisabledCount;
        private Integer userCreatedCount;
        private Integer userUpdatedCount;
        private Integer userDisabledCount;
    }

    @lombok.Data
    private static class SyncPartResult {
        private Integer totalCount = 0;
        private Integer successCount = 0;
        private Integer failCount = 0;
        private String errorMessage = "";
        private Integer createdCount = 0;
        private Integer updatedCount = 0;
        private Integer disabledCount = 0;

        public void appendError(String error) {
            if (error == null) {
                return;
            }
            this.errorMessage = this.errorMessage + error;
        }
    }
}
