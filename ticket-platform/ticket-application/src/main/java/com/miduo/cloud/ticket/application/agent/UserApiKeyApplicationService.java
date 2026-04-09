package com.miduo.cloud.ticket.application.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.miduo.cloud.ticket.common.constants.AgentApiKeyConstants;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.agent.UserApiKeyCreateInput;
import com.miduo.cloud.ticket.entity.dto.agent.UserApiKeyCreateOutput;
import com.miduo.cloud.ticket.entity.dto.agent.UserApiKeyListOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserApiKeyMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserApiKeyPO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 个人 API 密钥管理
 */
@Service
public class UserApiKeyApplicationService {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_DISABLED = 0;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Resource
    private SysUserApiKeyMapper sysUserApiKeyMapper;

    @Transactional(rollbackFor = Exception.class)
    public UserApiKeyCreateOutput createKey(Long userId, UserApiKeyCreateInput input) {
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        String keyUuid = UUID.randomUUID().toString().replace("-", "");
        String secretPart = UUID.randomUUID().toString().replace("-", "");
        String plaintext = AgentApiKeyConstants.KEY_PREFIX_LABEL + keyUuid + "_" + secretPart;

        SysUserApiKeyPO po = new SysUserApiKeyPO();
        po.setUserId(userId);
        po.setName(input.getName().trim());
        po.setKeyPrefix(keyUuid);
        po.setSecretHash(passwordEncoder.encode(plaintext));
        po.setStatus(STATUS_ACTIVE);
        po.setCreateBy(SecurityUtil.getCurrentUsername());
        po.setUpdateBy(SecurityUtil.getCurrentUsername());
        sysUserApiKeyMapper.insert(po);

        return new UserApiKeyCreateOutput(po.getId(), plaintext);
    }

    public List<UserApiKeyListOutput> listKeys(Long userId) {
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        LambdaQueryWrapper<SysUserApiKeyPO> q = new LambdaQueryWrapper<>();
        q.eq(SysUserApiKeyPO::getUserId, userId);
        q.orderByDesc(SysUserApiKeyPO::getCreateTime);
        List<SysUserApiKeyPO> list = sysUserApiKeyMapper.selectList(q);
        return list.stream().map(this::toListOutput).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableKey(Long userId, Long keyId) {
        assertOwner(userId, keyId);
        LambdaUpdateWrapper<SysUserApiKeyPO> u = new LambdaUpdateWrapper<>();
        u.eq(SysUserApiKeyPO::getId, keyId);
        u.set(SysUserApiKeyPO::getStatus, STATUS_DISABLED);
        u.set(SysUserApiKeyPO::getUpdateBy, SecurityUtil.getCurrentUsername());
        sysUserApiKeyMapper.update(null, u);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteKey(Long userId, Long keyId) {
        assertOwner(userId, keyId);
        sysUserApiKeyMapper.deleteById(keyId);
    }

    /**
     * 校验 API Key 明文，返回匹配且启用的记录；失败返回 null
     */
    public SysUserApiKeyPO validatePlaintextKey(String plaintext) {
        if (plaintext == null || !plaintext.startsWith(AgentApiKeyConstants.KEY_PREFIX_LABEL)) {
            return null;
        }
        String withoutPrefix = plaintext.substring(AgentApiKeyConstants.KEY_PREFIX_LABEL.length());
        int us = withoutPrefix.indexOf('_');
        if (us <= 0 || us >= withoutPrefix.length() - 1) {
            return null;
        }
        String prefix = withoutPrefix.substring(0, us);
        LambdaQueryWrapper<SysUserApiKeyPO> q = new LambdaQueryWrapper<>();
        q.eq(SysUserApiKeyPO::getKeyPrefix, prefix);
        q.eq(SysUserApiKeyPO::getStatus, STATUS_ACTIVE);
        SysUserApiKeyPO row = sysUserApiKeyMapper.selectOne(q);
        if (row == null || row.getSecretHash() == null) {
            return null;
        }
        if (!passwordEncoder.matches(plaintext, row.getSecretHash())) {
            return null;
        }
        return row;
    }

    /**
     * 异步更新最后使用时间，避免拖慢 Agent 请求主路径
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void touchLastUsed(Long keyId) {
        if (keyId == null) {
            return;
        }
        LambdaUpdateWrapper<SysUserApiKeyPO> u = new LambdaUpdateWrapper<>();
        u.eq(SysUserApiKeyPO::getId, keyId);
        u.set(SysUserApiKeyPO::getLastUsedAt, LocalDateTime.now());
        sysUserApiKeyMapper.update(null, u);
    }

    private void assertOwner(Long userId, Long keyId) {
        if (userId == null || keyId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        SysUserApiKeyPO po = sysUserApiKeyMapper.selectById(keyId);
        if (po == null || !userId.equals(po.getUserId())) {
            throw BusinessException.of(ErrorCode.FORBIDDEN);
        }
    }

    private UserApiKeyListOutput toListOutput(SysUserApiKeyPO po) {
        UserApiKeyListOutput o = new UserApiKeyListOutput();
        o.setId(po.getId());
        o.setName(po.getName());
        o.setKeyPrefixDisplay(AgentApiKeyConstants.KEY_PREFIX_LABEL + po.getKeyPrefix() + "…");
        o.setStatus(po.getStatus());
        o.setLastUsedAt(po.getLastUsedAt());
        o.setCreateTime(po.getCreateTime());
        return o;
    }
}
