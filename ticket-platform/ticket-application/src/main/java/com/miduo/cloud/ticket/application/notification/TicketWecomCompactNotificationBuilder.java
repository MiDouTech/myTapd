package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.util.TicketWecomCompactNotificationFormat;
import com.miduo.cloud.ticket.infrastructure.config.TicketLinkProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketBugInfoMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketBugInfoPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.springframework.stereotype.Component;

/**
 * 组装企微群工单事件通知的紧凑单行正文。
 */
@Component
public class TicketWecomCompactNotificationBuilder {

    private final TicketLinkProperties ticketLinkProperties;
    private final TicketBugInfoMapper ticketBugInfoMapper;

    public TicketWecomCompactNotificationBuilder(TicketLinkProperties ticketLinkProperties,
                                                  TicketBugInfoMapper ticketBugInfoMapper) {
        this.ticketLinkProperties = ticketLinkProperties;
        this.ticketBugInfoMapper = ticketBugInfoMapper;
    }

    public String build(TicketPO ticket) {
        if (ticket == null) {
            return TicketWecomCompactNotificationFormat.build("-", "-", "-", "");
        }
        String detailLink = ticketLinkProperties.buildOpenTicketLink(ticket.getTicketNo());
        TicketBugInfoPO bugInfo = ticketBugInfoMapper.selectOne(
                new LambdaQueryWrapper<TicketBugInfoPO>()
                        .eq(TicketBugInfoPO::getTicketId, ticket.getId())
                        .last("LIMIT 1"));
        return TicketWecomCompactNotificationFormat.build(
                ticket.getTicketNo(),
                resolveStatusLabel(ticket.getStatus()),
                bugInfo == null ? null : bugInfo.getCompanyName(),
                ticket.getTitle(),
                detailLink);
    }

    public String buildCommentMention(TicketPO ticket, String authorName, String commentSummary) {
        if (ticket == null) {
            return TicketWecomCompactNotificationFormat.buildCommentMention("-", authorName, commentSummary, "");
        }
        String detailLink = ticketLinkProperties.buildOpenTicketLink(ticket.getTicketNo());
        return TicketWecomCompactNotificationFormat.buildCommentMention(
                ticket.getTicketNo(), authorName, commentSummary, detailLink);
    }

    private String resolveStatusLabel(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "-";
        }
        TicketStatus status = TicketStatus.fromCode(code.toLowerCase());
        return status != null ? status.getLabel() : code;
    }
}
