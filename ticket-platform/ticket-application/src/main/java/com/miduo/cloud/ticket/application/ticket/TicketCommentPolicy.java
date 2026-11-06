package com.miduo.cloud.ticket.application.ticket;

import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 内部系统采用登录用户访问工单的现有模型；内部评论额外要求实时有效的企微员工身份。
 * 不信任 JWT 中的历史账号状态、插件用户映射或客户端自报身份。
 * 个人 API Key 的主体是其所属员工，沿用相同的内部访问规则。
 */
@Component
public class TicketCommentPolicy {
    private final SysUserMapper userMapper;

    public TicketCommentPolicy(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public boolean canAccessInternal(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUserPO user = userMapper.selectById(userId);
        return user != null && Integer.valueOf(1).equals(user.getAccountStatus())
                && !Integer.valueOf(1).equals(user.getDeleted())
                && StringUtils.hasText(user.getWecomUserid());
    }

    public void requireInternalAccess(Long userId) {
        if (!canAccessInternal(userId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "仅已绑定企业微信且启用的内部员工可操作评论");
        }
    }
}
