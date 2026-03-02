package com.miduo.cloud.ticket.application.user;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企微通讯录同步服务
 * 全量同步部门和成员数据
 */
@Service
public class WecomSyncService extends BaseApplicationService {

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
        log.info("开始全量同步企微通讯录...");

        syncDepartments();
        syncUsers();

        log.info("企微通讯录全量同步完成");
    }

    /**
     * 同步部门
     */
    private void syncDepartments() {
        log.info("开始同步企微部门...");
        List<WecomClient.WecomDepartment> wecomDepts = wecomClient.getDepartmentList();

        Map<Long, Department> existingMap = departmentRepository.findAll().stream()
                .filter(d -> d.getWecomDeptId() != null)
                .collect(Collectors.toMap(Department::getWecomDeptId, Function.identity(), (a, b) -> a));

        int created = 0;
        int updated = 0;

        for (WecomClient.WecomDepartment wecomDept : wecomDepts) {
            Department existing = existingMap.get(wecomDept.getId());

            if (existing != null) {
                boolean changed = false;
                if (!wecomDept.getName().equals(existing.getName())) {
                    existing.setName(wecomDept.getName());
                    changed = true;
                }

                Long localParentId = resolveParentId(wecomDept.getParentId(), existingMap);
                if (!java.util.Objects.equals(localParentId, existing.getParentId())) {
                    existing.setParentId(localParentId);
                    changed = true;
                }
                if (wecomDept.getOrder() != null && !wecomDept.getOrder().equals(existing.getSortOrder())) {
                    existing.setSortOrder(wecomDept.getOrder());
                    changed = true;
                }

                if (changed) {
                    departmentRepository.save(existing);
                    updated++;
                }
            } else {
                Department newDept = new Department();
                newDept.setName(wecomDept.getName());
                newDept.setWecomDeptId(wecomDept.getId());
                newDept.setSortOrder(wecomDept.getOrder() != null ? wecomDept.getOrder() : 0);

                Department saved = departmentRepository.save(newDept);
                existingMap.put(wecomDept.getId(), saved);
                created++;
            }
        }

        for (WecomClient.WecomDepartment wecomDept : wecomDepts) {
            if (wecomDept.getParentId() != null && wecomDept.getParentId() > 0) {
                Department current = existingMap.get(wecomDept.getId());
                Department parent = existingMap.get(wecomDept.getParentId());
                if (current != null && parent != null && !parent.getId().equals(current.getParentId())) {
                    current.setParentId(parent.getId());
                    departmentRepository.save(current);
                }
            }
        }

        log.info("部门同步完成: 新增={}, 更新={}", created, updated);
    }

    private Long resolveParentId(Long wecomParentId, Map<Long, Department> existingMap) {
        if (wecomParentId == null || wecomParentId <= 0) {
            return null;
        }
        Department parent = existingMap.get(wecomParentId);
        return parent != null ? parent.getId() : null;
    }

    /**
     * 同步用户（遍历所有部门的成员）
     */
    private void syncUsers() {
        log.info("开始同步企微用户...");

        List<Department> allDepts = departmentRepository.findAll();
        Map<Long, Department> wecomDeptMap = allDepts.stream()
                .filter(d -> d.getWecomDeptId() != null)
                .collect(Collectors.toMap(Department::getWecomDeptId, Function.identity(), (a, b) -> a));

        Set<String> processedUserIds = new HashSet<>();
        int created = 0;
        int updated = 0;

        for (Department dept : allDepts) {
            if (dept.getWecomDeptId() == null) {
                continue;
            }

            List<WecomClient.WecomUserDetail> wecomUsers;
            try {
                wecomUsers = wecomClient.getDepartmentUsers(dept.getWecomDeptId());
            } catch (Exception e) {
                log.warn("获取部门{}成员失败，跳过: {}", dept.getWecomDeptId(), e.getMessage());
                continue;
            }

            for (WecomClient.WecomUserDetail wecomUser : wecomUsers) {
                if (processedUserIds.contains(wecomUser.getUserId())) {
                    continue;
                }
                processedUserIds.add(wecomUser.getUserId());

                User existing = userRepository.findByWecomUserid(wecomUser.getUserId());

                if (existing != null) {
                    boolean changed = false;
                    if (wecomUser.getName() != null && !wecomUser.getName().equals(existing.getName())) {
                        existing.setName(wecomUser.getName());
                        changed = true;
                    }
                    if (wecomUser.getMobile() != null && !wecomUser.getMobile().equals(existing.getPhone())) {
                        existing.setPhone(wecomUser.getMobile());
                        changed = true;
                    }
                    if (wecomUser.getEmail() != null && !wecomUser.getEmail().equals(existing.getEmail())) {
                        existing.setEmail(wecomUser.getEmail());
                        changed = true;
                    }
                    if (wecomUser.getPosition() != null && !wecomUser.getPosition().equals(existing.getPosition())) {
                        existing.setPosition(wecomUser.getPosition());
                        changed = true;
                    }
                    if (wecomUser.getAvatar() != null && !wecomUser.getAvatar().equals(existing.getAvatarUrl())) {
                        existing.setAvatarUrl(wecomUser.getAvatar());
                        changed = true;
                    }

                    Long mainDeptId = resolveMainDepartment(wecomUser.getMainDepartment(), wecomDeptMap);
                    if (mainDeptId != null && !mainDeptId.equals(existing.getDepartmentId())) {
                        existing.setDepartmentId(mainDeptId);
                        changed = true;
                    }

                    Integer mappedStatus = mapWecomStatus(wecomUser.getStatus());
                    if (!mappedStatus.equals(existing.getAccountStatus())) {
                        existing.setAccountStatus(mappedStatus);
                        changed = true;
                    }

                    if (changed) {
                        userRepository.save(existing);
                        updated++;
                    }
                } else {
                    User newUser = new User();
                    newUser.setName(wecomUser.getName());
                    newUser.setPhone(wecomUser.getMobile());
                    newUser.setEmail(wecomUser.getEmail());
                    newUser.setPosition(wecomUser.getPosition());
                    newUser.setAvatarUrl(wecomUser.getAvatar());
                    newUser.setWecomUserid(wecomUser.getUserId());
                    newUser.setAccountStatus(mapWecomStatus(wecomUser.getStatus()));

                    Long mainDeptId = resolveMainDepartment(wecomUser.getMainDepartment(), wecomDeptMap);
                    newUser.setDepartmentId(mainDeptId);

                    User saved = userRepository.save(newUser);
                    userRepository.assignRole(saved.getId(), 4L);
                    created++;
                }
            }
        }

        log.info("用户同步完成: 新增={}, 更新={}", created, updated);
    }

    private Long resolveMainDepartment(Long wecomDeptId, Map<Long, Department> wecomDeptMap) {
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
            default:
                return 4;
        }
    }
}
