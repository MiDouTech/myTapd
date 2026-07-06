package com.miduo.cloud.ticket.application.plugin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.UserRole;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.plugin.mapper.PluginExternalUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.plugin.po.PluginExternalUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.SysUserQueryPriority;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysRoleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserRoleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysRolePO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserRolePO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 插件外部用户映射到本地 sys_user
 */
@Service
public class PluginUserMappingService {

    private static final Logger log = LoggerFactory.getLogger(PluginUserMappingService.class);
    private static final String EMPLOYEE_NO_PREFIX = "plugin:";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PluginExternalUserMapper pluginExternalUserMapper;

    public PluginUserMappingService(SysUserMapper sysUserMapper,
                                    SysRoleMapper sysRoleMapper,
                                    SysUserRoleMapper sysUserRoleMapper,
                                    PluginExternalUserMapper pluginExternalUserMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.pluginExternalUserMapper = pluginExternalUserMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long resolveOrCreateUser(String systemCode, String externalUserId, String userName, String dept, String mobile) {
        if (!StringUtils.hasText(systemCode) || !StringUtils.hasText(externalUserId)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "外部用户标识不完整");
        }
        String normalizedSystemCode = systemCode.trim();
        String normalizedExternalUserId = externalUserId.trim();
        String legacyEmployeeNo = buildEmployeeNo(normalizedSystemCode, normalizedExternalUserId);

        Long mappedUserId = findMappedUserId(normalizedSystemCode, normalizedExternalUserId);
        if (mappedUserId != null) {
            SysUserPO mappedUser = sysUserMapper.selectById(mappedUserId);
            if (mappedUser != null) {
                updateUserProfile(mappedUser, userName, dept, mobile);
                return mappedUser.getId();
            }
        }

        SysUserPO legacyUser = findByEmployeeNo(legacyEmployeeNo);
        if (legacyUser != null) {
            saveMapping(normalizedSystemCode, normalizedExternalUserId, legacyUser.getId());
            updateUserProfile(legacyUser, userName, dept, mobile);
            return legacyUser.getId();
        }

        SysUserPO preferredUser = findPreferredUserByPhone(mobile);
        if (preferredUser != null) {
            saveMapping(normalizedSystemCode, normalizedExternalUserId, preferredUser.getId());
            updateUserProfile(preferredUser, userName, dept, mobile);
            assignGuestRole(preferredUser.getId());
            log.info("插件用户绑定已有本地账号: systemCode={}, externalUserId={}, userId={}",
                    normalizedSystemCode, normalizedExternalUserId, preferredUser.getId());
            return preferredUser.getId();
        }

        SysUserPO user = new SysUserPO();
        user.setName(StringUtils.hasText(userName) ? userName.trim() : normalizedExternalUserId);
        user.setEmployeeNo(legacyEmployeeNo);
        user.setPhone(resolvePhoneForNewUser(mobile));
        user.setPosition(trimToNull(dept));
        user.setAccountStatus(1);
        user.setSyncStatus(0);
        user.setCreateBy("plugin");
        user.setUpdateBy("plugin");
        sysUserMapper.insert(user);
        saveMapping(normalizedSystemCode, normalizedExternalUserId, user.getId());
        assignGuestRole(user.getId());
        return user.getId();
    }

    private Long findMappedUserId(String systemCode, String externalUserId) {
        PluginExternalUserPO mapping = pluginExternalUserMapper.selectOne(new LambdaQueryWrapper<PluginExternalUserPO>()
                .eq(PluginExternalUserPO::getSystemCode, systemCode)
                .eq(PluginExternalUserPO::getExternalUserId, externalUserId)
                .last("LIMIT 1"));
        return mapping == null ? null : mapping.getUserId();
    }

    private void saveMapping(String systemCode, String externalUserId, Long userId) {
        if (userId == null) {
            return;
        }
        PluginExternalUserPO existing = pluginExternalUserMapper.selectOne(new LambdaQueryWrapper<PluginExternalUserPO>()
                .eq(PluginExternalUserPO::getSystemCode, systemCode)
                .eq(PluginExternalUserPO::getExternalUserId, externalUserId)
                .last("LIMIT 1"));
        if (existing != null) {
            if (!userId.equals(existing.getUserId())) {
                existing.setUserId(userId);
                existing.setUpdateBy("plugin");
                pluginExternalUserMapper.updateById(existing);
            }
            return;
        }
        PluginExternalUserPO mapping = new PluginExternalUserPO();
        mapping.setSystemCode(systemCode);
        mapping.setExternalUserId(externalUserId);
        mapping.setUserId(userId);
        mapping.setCreateBy("plugin");
        mapping.setUpdateBy("plugin");
        pluginExternalUserMapper.insert(mapping);
    }

    private SysUserPO findPreferredUserByPhone(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return null;
        }
        List<SysUserPO> candidates = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getPhone, mobile.trim()));
        SysUserPO preferred = SysUserQueryPriority.pickPreferred(candidates);
        if (candidates != null && candidates.size() > 1) {
            log.warn("插件用户手机号命中多条本地账号，优先绑定企微账号: mobile={}, chosenUserId={}",
                    mobile.trim(), preferred == null ? null : preferred.getId());
        }
        return preferred;
    }

    private String resolvePhoneForNewUser(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return null;
        }
        String normalizedMobile = mobile.trim();
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getPhone, normalizedMobile));
        if (count != null && count > 0) {
            log.warn("插件新建用户跳过冲突手机号: mobile={}", normalizedMobile);
            return null;
        }
        return normalizedMobile;
    }

    private SysUserPO findByEmployeeNo(String employeeNo) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getEmployeeNo, employeeNo)
                .last("LIMIT 1"));
    }

    private void updateUserProfile(SysUserPO user, String userName, String dept, String mobile) {
        if (user == null) {
            return;
        }
        boolean changed = false;
        boolean wecomLinked = hasWecomUserid(user);

        if (!wecomLinked && StringUtils.hasText(userName) && !userName.trim().equals(user.getName())) {
            user.setName(userName.trim());
            changed = true;
        }
        if (StringUtils.hasText(dept) && !dept.trim().equals(user.getPosition())) {
            user.setPosition(dept.trim());
            changed = true;
        }
        if (!wecomLinked && StringUtils.hasText(mobile)) {
            String normalizedMobile = mobile.trim();
            if (!normalizedMobile.equals(user.getPhone()) && !phoneUsedByOtherUser(normalizedMobile, user.getId())) {
                user.setPhone(normalizedMobile);
                changed = true;
            }
        }
        if (changed) {
            user.setUpdateBy("plugin");
            sysUserMapper.updateById(user);
        }
    }

    private boolean phoneUsedByOtherUser(String phone, Long currentUserId) {
        LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getPhone, phone);
        if (currentUserId != null) {
            wrapper.ne(SysUserPO::getId, currentUserId);
        }
        Long count = sysUserMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private boolean hasWecomUserid(SysUserPO user) {
        return user != null && StringUtils.hasText(user.getWecomUserid());
    }

    private void assignGuestRole(Long userId) {
        SysRolePO guestRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRolePO>()
                .eq(SysRolePO::getRoleCode, UserRole.GUEST.getCode())
                .last("LIMIT 1"));
        if (guestRole == null) {
            return;
        }
        Long count = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRolePO>()
                .eq(SysUserRolePO::getUserId, userId)
                .eq(SysUserRolePO::getRoleId, guestRole.getId()));
        if (count != null && count > 0) {
            return;
        }
        SysUserRolePO relation = new SysUserRolePO();
        relation.setUserId(userId);
        relation.setRoleId(guestRole.getId());
        relation.setCreateBy("plugin");
        relation.setUpdateBy("plugin");
        sysUserRoleMapper.insert(relation);
    }

    private String buildEmployeeNo(String systemCode, String externalUserId) {
        return EMPLOYEE_NO_PREFIX + systemCode + ":" + externalUserId;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
