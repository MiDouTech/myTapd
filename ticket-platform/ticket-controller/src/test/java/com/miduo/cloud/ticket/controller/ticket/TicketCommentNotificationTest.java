package com.miduo.cloud.ticket.controller.ticket;

import com.miduo.cloud.ticket.application.notification.*;
import com.miduo.cloud.ticket.application.notification.sender.*;
import com.miduo.cloud.ticket.application.ticket.*;
import com.miduo.cloud.ticket.application.webhook.*;
import com.miduo.cloud.ticket.common.enums.*;
import com.miduo.cloud.ticket.domain.common.event.TicketCommentMentionEvent;
import com.miduo.cloud.ticket.infrastructure.config.TicketLinkProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper.NotificationPreferenceMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TicketCommentNotificationTest {
    private NotificationOrchestrator notifications;
    private WebhookDispatchService webhooks;
    private TicketEventNotificationListener listener;
    private TicketWebhookEventListener webhookListener;
    private TicketCommentMapper comments;
    private SysUserMapper users;

    @BeforeEach
    void setup() {
        notifications = mock(NotificationOrchestrator.class);
        webhooks = mock(WebhookDispatchService.class);
        TicketMapper tickets = mock(TicketMapper.class);
        TicketPO ticket = new TicketPO();
        ticket.setId(42L);
        when(tickets.selectById(42L)).thenReturn(ticket);
        comments = mock(TicketCommentMapper.class);
        users = mock(SysUserMapper.class);
        SysUserPO recipient = new SysUserPO();
        recipient.setAccountStatus(1);
        recipient.setWecomUserid("employee-8");
        when(users.selectById(8L)).thenReturn(recipient);
        listener = new TicketEventNotificationListener(notifications, mock(WecomGroupPushService.class),
                webhooks, tickets, mock(TicketFollowerMapper.class), users, mock(TicketAssigneeSyncService.class),
                mock(TicketLinkProperties.class), mock(TicketWecomCompactNotificationBuilder.class));
        ReflectionTestUtils.setField(listener, "commentPolicy", new TicketCommentPolicy(users));
        ReflectionTestUtils.setField(listener, "commentMapper", comments);
        webhookListener = new TicketWebhookEventListener(webhooks, mock(TicketAssigneeSyncService.class));
        ReflectionTestUtils.setField(webhookListener, "commentMapper", comments);
    }

    @Test
    void internalMentionOnlyUsesSiteAndRechecksRecipient() {
        row("INTERNAL");
        TicketCommentMentionEvent event = event("INTERNAL");
        listener.onTicketCommentMention(event);
        webhookListener.onTicketCommentMention(event);
        verify(notifications).dispatchInternalCommentMention(8L, 42L);
        verifyNoMoreInteractions(notifications);
        verifyNoInteractions(webhooks);
        reset(notifications);
        when(users.selectById(8L)).thenReturn(null);
        listener.onTicketCommentMention(event);
        verifyNoInteractions(notifications);
    }

    @Test
    void persistedVisibilityPreventsForgedPublicEventsAndUnknownEvents() {
        row("INTERNAL");
        listener.onTicketCommentMention(event("PUBLIC"));
        webhookListener.onTicketCommentMention(event("PUBLIC"));
        webhookListener.onTicketCommentMention(new TicketCommentMentionEvent(42L, null, null,
                Collections.singletonList(8L), 7L, "private"));
        verifyNoInteractions(notifications, webhooks);
    }

    @Test
    void publicMentionStillDispatchesWebhook() {
        row("PUBLIC");
        webhookListener.onTicketCommentMention(event("PUBLIC"));
        verify(webhooks).dispatch(eq(WebhookEventType.TICKET_COMMENT_MENTION), eq(42L), any());
    }

    @Test
    void internalOrchestratorCannotSendEmailOrWecomOrBody() {
        SiteNotificationSender site = mock(SiteNotificationSender.class);
        NotificationSender external = mock(NotificationSender.class);
        NotificationOrchestrator orchestrator = new NotificationOrchestrator(Collections.singletonList(external),
                site, mock(NotificationPreferenceMapper.class), mock(SystemConfigMapper.class), mock(StringRedisTemplate.class));
        orchestrator.dispatchInternalCommentMention(8L, 42L);
        verify(site).send(8L, 42L, null, NotificationType.COMMENT_MENTION.getCode(), "内部备注提醒", "有一条内部备注提及你");
        verifyNoInteractions(external);
    }

    private void row(String visibility) {
        TicketCommentPO comment = new TicketCommentPO();
        comment.setId(100L);
        comment.setTicketId(42L);
        comment.setVisibility(visibility);
        when(comments.selectById(100L)).thenReturn(comment);
    }

    private TicketCommentMentionEvent event(String visibility) {
        return new TicketCommentMentionEvent(42L, 100L, visibility, Collections.singletonList(8L), 7L, "private-body");
    }
}
