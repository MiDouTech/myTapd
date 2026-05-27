package com.miduo.cloud.ticket.application.bugreport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.domain.common.event.TicketCompletedEvent;
import com.miduo.cloud.ticket.domain.common.event.TicketStatusChangedEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketFlowRecordMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketFlowRecordPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.springframework.util.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bug简报事件监听器
 */
@Component
public class BugReportEventListener {

    private final BugReportApplicationService bugReportApplicationService;
    private final TicketMapper ticketMapper;
    private final TicketFlowRecordMapper ticketFlowRecordMapper;

    public BugReportEventListener(BugReportApplicationService bugReportApplicationService,
                                  TicketMapper ticketMapper,
                                  TicketFlowRecordMapper ticketFlowRecordMapper) {
        this.bugReportApplicationService = bugReportApplicationService;
        this.ticketMapper = ticketMapper;
        this.ticketFlowRecordMapper = ticketFlowRecordMapper;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketCompleted(TicketCompletedEvent event) {
        // 「已关闭」通常为无效/非缺陷收尾，不强制生成简报草稿与关联。
        // 但若工单曾进入「已完成」后又回退再关闭，仍需补齐简报闭环。
        if (StringUtils.hasText(event.getFinalStatus())
                && TicketStatus.CLOSED.getCode().equalsIgnoreCase(event.getFinalStatus().trim())
                && !hasReachedCompleted(event.getTicketId())) {
            return;
        }
        bugReportApplicationService.createDraftFromClosedTicket(event.getTicketId(), event.getOperatorId());
    }

    /**
     * 缺陷工单进入「临时解决」后也自动建立（或复用）简报关联，
     * 让工单详情在该状态下可直接展示并跳转到关联 Bug 简报。
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        if (!StringUtils.hasText(event.getNewStatus())) {
            return;
        }
        if (!TicketStatus.TEMP_RESOLVED.getCode().equalsIgnoreCase(event.getNewStatus().trim())) {
            return;
        }
        bugReportApplicationService.createDraftFromClosedTicket(event.getTicketId(), event.getOperatorId());
    }

    private boolean hasReachedCompleted(Long ticketId) {
        if (ticketId == null) {
            return false;
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket != null && ticket.getResolvedAt() != null) {
            return true;
        }
        Long completedCount = ticketFlowRecordMapper.selectCount(
                new LambdaQueryWrapper<TicketFlowRecordPO>()
                        .eq(TicketFlowRecordPO::getTicketId, ticketId)
                        .eq(TicketFlowRecordPO::getToStatus, TicketStatus.COMPLETED.getCode()));
        return completedCount != null && completedCount > 0;
    }
}
