package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user;

import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;

import java.util.Comparator;
import java.util.List;

/**
 * 用户查询优先级：企微账号优先，其次已激活账号，最后按 ID 最早。
 */
public final class SysUserQueryPriority {

    private SysUserQueryPriority() {
    }

    public static SysUserPO pickPreferred(List<SysUserPO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return candidates.stream()
                .filter(user -> user != null)
                .min(Comparator
                        .comparing(SysUserQueryPriority::hasWecomUserid).reversed()
                        .thenComparing(SysUserQueryPriority::isActive).reversed()
                        .thenComparing(SysUserPO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private static int hasWecomUserid(SysUserPO user) {
        if (user == null || user.getWecomUserid() == null) {
            return 0;
        }
        return user.getWecomUserid().trim().isEmpty() ? 0 : 1;
    }

    private static int isActive(SysUserPO user) {
        return user != null && Integer.valueOf(1).equals(user.getAccountStatus()) ? 1 : 0;
    }
}
