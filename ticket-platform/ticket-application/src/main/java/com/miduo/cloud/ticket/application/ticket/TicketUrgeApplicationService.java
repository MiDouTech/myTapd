package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.TicketAction;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.common.event.TicketUrgedEvent;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketUrgeInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 工单催办应用服务
 */
@Service
public class TicketUrgeApplicationService extends BaseApplicationService {

    private final TicketMapper ticketMapper;
    private final TicketAssigneeSyncService ticketAssigneeSyncService;
    private final SysUserMapper sysUserMapper;
    private final TicketLogMapper ticketLogMapper;
    private final ApplicationEventPublisher eventPublisher;

    public TicketUrgeApplicationService(TicketMapper ticketMapper,
                                        TicketAssigneeSyncService ticketAssigneeSyncService,
                                        SysUserMapper sysUserMapper,
                                        TicketLogMapper ticketLogMapper,
                                        ApplicationEventPublisher eventPublisher) {
        this.ticketMapper = ticketMapper;
        this.ticketAssigneeSyncService = ticketAssigneeSyncService;
        this.sysUserMapper = sysUserMapper;
        this.ticketLogMapper = ticketLogMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 按工单ID催办（默认通知全部关联处理人，可追加 extraNotifyUserIds）
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketPO urgeByTicketId(Long ticketId, Long urgerId, TicketUrgeInput input) {
        if (ticketId == null || ticketId <= 0) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "工单ID不能为空");
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        List<Long> extra = input != null && input.getExtraNotifyUserIds() != null
                ? input.getExtraNotifyUserIds()
                : Collections.emptyList();
        publishUrgeEvent(ticket, urgerId, extra);
        return ticket;
    }

    /**
     * 按工单编号催办（企微等场景：仅默认处理人，无额外通知人）
     */
    @Transactional(rollbackFor = Exception.class)
    public TicketPO urgeByTicketNo(String ticketNo, Long urgerId) {
        if (ticketNo == null || ticketNo.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "工单编号不能为空");
        }

        LambdaQueryWrapper<TicketPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketPO::getTicketNo, ticketNo.trim())
                .last("LIMIT 1");
        TicketPO ticket = ticketMapper.selectOne(wrapper);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND, "工单不存在: " + ticketNo);
        }
        publishUrgeEvent(ticket, urgerId, Collections.emptyList());
        return ticket;
    }

    private void publishUrgeEvent(TicketPO ticket, Long urgerId, List<Long> extraNotifyUserIds) {
        assertUrgeAllowedStatus(ticket);

        List<Long> defaultNotify = ticketAssigneeSyncService.listActiveUserIds(ticket.getId());
        if (defaultNotify.isEmpty() && ticket.getAssigneeId() != null) {
            defaultNotify = new ArrayList<>(Collections.singletonList(ticket.getAssigneeId()));
        }
        if (defaultNotify.isEmpty()) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "工单暂无处理人，无法催办");
        }

        Set<Long> merged = new LinkedHashSet<>(defaultNotify);
        if (extraNotifyUserIds != null) {
            for (Long uid : extraNotifyUserIds) {
                if (uid != null && uid > 0) {
                    merged.add(uid);
                }
            }
        }
        List<Long> notifyUserIds = new ArrayList<>(merged);
        validateNotifyUsersExist(notifyUserIds);

        int affected = ticketMapper.incrementUrgeCount(ticket.getId());
        if (affected != 1) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND, "工单不存在或已删除");
        }
        TicketPO afterCount = ticketMapper.selectById(ticket.getId());
        int newCount = afterCount != null && afterCount.getUrgeCount() != null ? afterCount.getUrgeCount() : 1;
        int oldCount = Math.max(0, newCount - 1);
        recordUrgeLog(ticket.getId(), urgerId, oldCount, newCount, notifyUserIds.size());

        eventPublisher.publishEvent(new TicketUrgedEvent(ticket.getId(), urgerId, notifyUserIds));
        log.info("工单催办事件已发布: ticketId={}, urgerId={}, notifyUserIds={}, urgeCount={}",
                ticket.getId(), urgerId, notifyUserIds, newCount);
    }

    private void recordUrgeLog(Long ticketId, Long urgerId, int oldCount, int newCount, int notifyUserCount) {
        TicketLogPO logPO = new TicketLogPO();
        logPO.setTicketId(ticketId);
        logPO.setUserId(urgerId != null ? urgerId : 0L);
        logPO.setAction(TicketAction.URGE.getCode());
        logPO.setOldValue(String.valueOf(oldCount));
        logPO.setNewValue(String.valueOf(newCount));
        logPO.setRemark("人工催办，通知人数 " + notifyUserCount);
        ticketLogMapper.insert(logPO);
    }

    private static void assertUrgeAllowedStatus(TicketPO ticket) {
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        if (status == null) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "工单状态不允许催办");
        }
        if (status.isTerminal()) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "终态工单不可催办");
        }
        if (status == TicketStatus.PENDING_ASSIGN) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "待分派工单请先分派处理人后再催办");
        }
    }

    private void validateNotifyUsersExist(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "通知对象不能为空");
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(userIds);
        if (users == null || users.size() != userIds.size()) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "部分通知用户不存在");
        }
    }
}
