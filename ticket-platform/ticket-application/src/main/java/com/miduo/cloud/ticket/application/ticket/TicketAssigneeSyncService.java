package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketAssigneeMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketAssigneePO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 工单多人处理人：与 ticket.assignee_id（首位主处理人）保持同步
 */
@Service
public class TicketAssigneeSyncService extends BaseApplicationService {

    private final TicketAssigneeMapper ticketAssigneeMapper;
    private final TicketMapper ticketMapper;
    private final SysUserMapper sysUserMapper;

    public TicketAssigneeSyncService(TicketAssigneeMapper ticketAssigneeMapper,
                                     TicketMapper ticketMapper,
                                     SysUserMapper sysUserMapper) {
        this.ticketAssigneeMapper = ticketAssigneeMapper;
        this.ticketMapper = ticketMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查询工单当前有效处理人 ID 列表（按 sort_order、id 排序）
     */
    public List<Long> listActiveUserIds(Long ticketId) {
        if (ticketId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<TicketAssigneePO> q = new LambdaQueryWrapper<>();
        q.eq(TicketAssigneePO::getTicketId, ticketId)
                .orderByAsc(TicketAssigneePO::getSortOrder)
                .orderByAsc(TicketAssigneePO::getId);
        List<TicketAssigneePO> rows = ticketAssigneeMapper.selectList(q);
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        for (TicketAssigneePO row : rows) {
            if (row.getUserId() != null) {
                ids.add(row.getUserId());
            }
        }
        return ids;
    }

    /**
     * 是否为主处理人或协同处理人
     */
    public boolean isAmongAssignees(Long ticketId, Long userId) {
        if (ticketId == null || userId == null) {
            return false;
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return false;
        }
        if (userId.equals(ticket.getAssigneeId())) {
            return true;
        }
        LambdaQueryWrapper<TicketAssigneePO> q = new LambdaQueryWrapper<>();
        q.eq(TicketAssigneePO::getTicketId, ticketId)
                .eq(TicketAssigneePO::getUserId, userId);
        Long cnt = ticketAssigneeMapper.selectCount(q);
        return cnt != null && cnt > 0;
    }

    /**
     * 替换明细行并设置 ticket 主处理人字段（由调用方执行 ticketMapper.updateById）
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyAssigneesToTicket(TicketPO ticket, List<Long> orderedUserIds) {
        if (ticket == null || ticket.getId() == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "工单不能为空");
        }
        Long ticketId = ticket.getId();

        List<Long> normalized = normalizeUserIdList(orderedUserIds);
        if (!normalized.isEmpty()) {
            List<SysUserPO> users = sysUserMapper.selectBatchIds(normalized);
            if (users == null || users.size() != normalized.size()) {
                throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "部分处理人不存在");
            }
        }

        LambdaQueryWrapper<TicketAssigneePO> del = new LambdaQueryWrapper<>();
        del.eq(TicketAssigneePO::getTicketId, ticketId);
        ticketAssigneeMapper.delete(del);

        int order = 0;
        for (Long uid : normalized) {
            TicketAssigneePO row = new TicketAssigneePO();
            row.setTicketId(ticketId);
            row.setUserId(uid);
            row.setSortOrder(order++);
            ticketAssigneeMapper.insert(row);
        }

        ticket.setAssigneeId(normalized.isEmpty() ? null : normalized.get(0));
    }

    /**
     * 加载工单后替换处理人并立即落库主表（非工作流路径使用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceAndPersist(Long ticketId, List<Long> orderedUserIds) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        applyAssigneesToTicket(ticket, orderedUserIds);
        ticketMapper.updateById(ticket);
    }

    public void syncSingleAssigneeRow(TicketPO ticket, Long userId) {
        List<Long> list = userId == null ? Collections.emptyList() : Collections.singletonList(userId);
        applyAssigneesToTicket(ticket, list);
    }

    private static List<Long> normalizeUserIdList(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : raw) {
            if (id != null) {
                seen.add(id);
            }
        }
        return new ArrayList<>(seen);
    }
}
