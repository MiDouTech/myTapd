package com.miduo.cloud.ticket.application.plugin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.UserRole;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysRoleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserRoleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysRolePO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserRolePO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 插件外部用户映射到本地 sys_user
 */
@Service
public class PluginUserMappingService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public PluginUserMappingService(SysUserMapper sysUserMapper,
                                    SysRoleMapper sysRoleMapper,
                                    SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long resolveOrCreateUser(String systemCode, String externalUserId, String userName, String dept, String mobile) {
        if (!StringUtils.hasText(systemCode) || !StringUtils.hasText(externalUserId)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "外部用户标识不完整");
        }
        String employeeNo = buildEmployeeNo(systemCode.trim(), externalUserId.trim());
        SysUserPO existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserPO>()
                .eq(SysUserPO::getEmployeeNo, employeeNo)
                .last("LIMIT 1"));
        if (existing != null) {
            updateUserProfile(existing, userName, dept, mobile);
            return existing.getId();
        }
        SysUserPO user = new SysUserPO();
        user.setName(StringUtils.hasText(userName) ? userName.trim() : externalUserId.trim());
        user.setEmployeeNo(employeeNo);
        user.setPhone(trimToNull(mobile));
        user.setPosition(trimToNull(dept));
        user.setAccountStatus(1);
        user.setSyncStatus(0);
        user.setCreateBy("plugin");
        user.setUpdateBy("plugin");
        sysUserMapper.insert(user);
        assignGuestRole(user.getId());
        return user.getId();
    }

    private void updateUserProfile(SysUserPO user, String userName, String dept, String mobile) {
        boolean changed = false;
        if (StringUtils.hasText(userName) && !userName.trim().equals(user.getName())) {
            user.setName(userName.trim());
            changed = true;
        }
        if (StringUtils.hasText(dept) && !dept.trim().equals(user.getPosition())) {
            user.setPosition(dept.trim());
            changed = true;
        }
        if (StringUtils.hasText(mobile) && !mobile.trim().equals(user.getPhone())) {
            user.setPhone(mobile.trim());
            changed = true;
        }
        if (changed) {
            user.setUpdateBy("plugin");
            sysUserMapper.updateById(user);
        }
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
        return "plugin:" + systemCode + ":" + externalUserId;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
