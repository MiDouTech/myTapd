package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.common.event.TicketUrgedEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * 工单催办应用服务
 */
@Service
public class TicketUrgeApplicationService extends BaseApplicationService {

    private final TicketMapper ticketMapper;
    private final ApplicationEventPublisher eventPublisher;

    public TicketUrgeApplicationService(TicketMapper ticketMapper,
                                        ApplicationEventPublisher eventPublisher) {
        this.ticketMapper = ticketMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 按工单ID催办
     */
    public TicketPO urgeByTicketId(Long ticketId, Long urgerId) {
        if (ticketId == null || ticketId <= 0) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "工单ID不能为空");
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        publishUrgeEvent(ticket, urgerId);
        return ticket;
    }

    /**
     * 按工单编号催办
     */
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
        publishUrgeEvent(ticket, urgerId);
        return ticket;
    }

    private void publishUrgeEvent(TicketPO ticket, Long urgerId) {
        if (ticket.getAssigneeId() == null) {
            throw BusinessException.of(ErrorCode.TICKET_STATUS_INVALID, "工单暂无处理人，无法催办");
        }
        eventPublisher.publishEvent(new TicketUrgedEvent(ticket.getId(), urgerId, ticket.getAssigneeId()));
        log.info("工单催办事件已发布: ticketId={}, urgerId={}, handlerId={}",
                ticket.getId(), urgerId, ticket.getAssigneeId());
    }
}
