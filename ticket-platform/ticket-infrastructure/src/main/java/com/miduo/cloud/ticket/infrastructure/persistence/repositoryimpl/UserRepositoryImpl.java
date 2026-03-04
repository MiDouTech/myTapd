package com.miduo.cloud.ticket.infrastructure.persistence.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserRoleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.model.UserRoleCodePair;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserRolePO;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户仓储实现
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public UserRepositoryImpl(SysUserMapper sysUserMapper, SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    @Override
    public User findById(Long id) {
        if (id == null) {
            return null;
        }
        SysUserPO po = sysUserMapper.selectById(id);
        if (po == null) {
            return null;
        }
        User user = convertToModel(po);
        user.setRoleCodes(sysUserMapper.selectRoleCodesByUserId(id));
        return user;
    }

    @Override
    public User findByWecomUserid(String wecomUserid) {
        if (wecomUserid == null || wecomUserid.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPO::getWecomUserid, wecomUserid);
        SysUserPO po = sysUserMapper.selectOne(wrapper);
        if (po == null) {
            return null;
        }
        User user = convertToModel(po);
        user.setRoleCodes(sysUserMapper.selectRoleCodesByUserId(po.getId()));
        return user;
    }

    @Override
    public List<User> findAllActive() {
        LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPO::getAccountStatus, 1);
        wrapper.orderByAsc(SysUserPO::getId);
        List<SysUserPO> list = sysUserMapper.selectList(wrapper);
        return list.stream().map(this::convertToModel).collect(Collectors.toList());
    }

    @Override
    public List<User> findAll() {
        LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysUserPO::getId);
        List<SysUserPO> list = sysUserMapper.selectList(wrapper);
        return list.stream().map(this::convertToModel).collect(Collectors.toList());
    }

    @Override
    public List<User> findByDepartmentId(Long departmentId) {
        if (departmentId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<SysUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPO::getDepartmentId, departmentId);
        wrapper.eq(SysUserPO::getAccountStatus, 1);
        wrapper.orderByAsc(SysUserPO::getId);
        List<SysUserPO> list = sysUserMapper.selectList(wrapper);
        return list.stream().map(this::convertToModel).collect(Collectors.toList());
    }

    @Override
    public List<User> findByDepartmentIds(List<Long> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysUserPO> list = sysUserMapper.selectByDepartmentIds(departmentIds);
        return list.stream().map(this::convertToModel).collect(Collectors.toList());
    }

    @Override
    public User save(User user) {
        if (user == null) {
            return null;
        }
        SysUserPO po = convertToPO(user);
        if (user.getId() != null) {
            sysUserMapper.updateById(po);
        } else {
            sysUserMapper.insert(po);
            user.setId(po.getId());
        }
        return user;
    }

    @Override
    public void batchSave(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        for (User user : users) {
            save(user);
        }
    }

    @Override
    public void updateAccountStatus(Long userId, Integer status) {
        if (userId == null || status == null) {
            return;
        }
        LambdaUpdateWrapper<SysUserPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUserPO::getId, userId);
        wrapper.set(SysUserPO::getAccountStatus, status);
        sysUserMapper.update(null, wrapper);
    }

    @Override
    public List<String> findRoleCodes(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return sysUserMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public Map<Long, List<String>> findRoleCodesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserRoleCodePair> pairs = sysUserMapper.selectRoleCodesByUserIds(userIds);
        if (pairs == null || pairs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<String>> roleCodeMap = new HashMap<>();
        for (UserRoleCodePair pair : pairs) {
            if (pair == null || pair.getUserId() == null || pair.getRoleCode() == null) {
                continue;
            }
            roleCodeMap.computeIfAbsent(pair.getUserId(), key -> new ArrayList<>()).add(pair.getRoleCode());
        }
        return roleCodeMap;
    }

    @Override
    public void assignRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return;
        }
        LambdaQueryWrapper<SysUserRolePO> check = new LambdaQueryWrapper<>();
        check.eq(SysUserRolePO::getUserId, userId);
        check.eq(SysUserRolePO::getRoleId, roleId);
        Long count = sysUserRoleMapper.selectCount(check);
        if (count != null && count > 0) {
            return;
        }
        SysUserRolePO po = new SysUserRolePO();
        po.setUserId(userId);
        po.setRoleId(roleId);
        sysUserRoleMapper.insert(po);
    }

    private User convertToModel(SysUserPO po) {
        User user = new User();
        user.setId(po.getId());
        user.setName(po.getName());
        user.setEmployeeNo(po.getEmployeeNo());
        user.setDepartmentId(po.getDepartmentId());
        user.setEmail(po.getEmail());
        user.setPhone(po.getPhone());
        user.setPosition(po.getPosition());
        user.setGender(po.getGender());
        user.setAvatarUrl(po.getAvatarUrl());
        user.setWecomUserid(po.getWecomUserid());
        user.setAccountStatus(po.getAccountStatus());
        user.setSyncStatus(po.getSyncStatus());
        user.setSyncTime(po.getSyncTime());
        user.setCreateTime(po.getCreateTime());
        user.setUpdateTime(po.getUpdateTime());
        return user;
    }

    private SysUserPO convertToPO(User user) {
        SysUserPO po = new SysUserPO();
        po.setId(user.getId());
        po.setName(user.getName());
        po.setEmployeeNo(user.getEmployeeNo());
        po.setDepartmentId(user.getDepartmentId());
        po.setEmail(user.getEmail());
        po.setPhone(user.getPhone());
        po.setPosition(user.getPosition());
        po.setGender(user.getGender());
        po.setAvatarUrl(user.getAvatarUrl());
        po.setWecomUserid(user.getWecomUserid());
        po.setAccountStatus(user.getAccountStatus());
        po.setSyncStatus(user.getSyncStatus());
        po.setSyncTime(user.getSyncTime());
        return po;
    }
}
