package com.miduo.cloud.ticket.domain.common.event;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 工单评论中 @ 提及用户后触发，用于事务提交后异步分发通知（站内 + 企微）
 */
@Getter
public class TicketCommentMentionEvent extends DomainEvent {

    private final Long ticketId;
    private final Long commentId;
    private final String visibility;
    private final List<Long> mentionedUserIds;
    private final Long commentAuthorUserId;
    private final String commentPlainSummary;

    public TicketCommentMentionEvent(Long ticketId,
                                     Long commentId,
                                     String visibility,
                                     List<Long> mentionedUserIds,
                                     Long commentAuthorUserId,
                                     String commentPlainSummary) {
        super();
        this.ticketId = ticketId;
        this.commentId = commentId;
        this.visibility = visibility;
        this.mentionedUserIds = mentionedUserIds != null
                ? Collections.unmodifiableList(new java.util.ArrayList<>(mentionedUserIds))
                : Collections.emptyList();
        this.commentAuthorUserId = commentAuthorUserId;
        this.commentPlainSummary = commentPlainSummary;
    }
}
