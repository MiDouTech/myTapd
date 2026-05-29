package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.miduo.cloud.ticket.common.enums.NotificationType;
import com.miduo.cloud.ticket.common.enums.SlaNotificationPendingStatus;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper.SlaNotificationPendingMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po.SlaNotificationPendingPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SLA 通知分发：静默时段入队，其余立即发送
 */
@Service
public class SlaNotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(SlaNotificationDispatchService.class);

    private final NotificationOrchestrator orchestrator;
    private final WecomGroupPushService wecomGroupPushService;
    private final SlaNotificationQuietHours quietHours;
    private final SlaNotificationPendingMapper pendingMapper;

    public SlaNotificationDispatchService(NotificationOrchestrator orchestrator,
                                          WecomGroupPushService wecomGroupPushService,
                                          SlaNotificationQuietHours quietHours,
                                          SlaNotificationPendingMapper pendingMapper) {
        this.orchestrator = orchestrator;
        this.wecomGroupPushService = wecomGroupPushService;
        this.quietHours = quietHours;
        this.pendingMapper = pendingMapper;
    }

    /**
     * 分发 SLA 预警/超时通知（按静默规则立即发送或延迟到次日 9:00）
     */
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(Long ticketId,
                         NotificationType type,
                         String title,
                         String content,
                         String detailLink,
                         Collection<Long> receiverUserIds,
                         Collection<Long> mentionUserIds) {
        if (ticketId == null || type == null) {
            return;
        }
        ZoneId zoneId = quietHours.resolveZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);

        if (quietHours.isQuietHours(now)) {
            enqueuePending(ticketId, type, title, content, detailLink, receiverUserIds, mentionUserIds, now);
            return;
        }
        sendImmediately(ticketId, type, title, content, detailLink, receiverUserIds, mentionUserIds);
    }

    /**
     * 发送已到期的延迟 SLA 通知
     */
    @Transactional(rollbackFor = Exception.class)
    public int flushDuePendingNotifications() {
        Date now = new Date();
        LambdaQueryWrapper<SlaNotificationPendingPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaNotificationPendingPO::getStatus, SlaNotificationPendingStatus.PENDING.getCode())
                .le(SlaNotificationPendingPO::getScheduledSendAt, now)
                .orderByAsc(SlaNotificationPendingPO::getScheduledSendAt)
                .last("LIMIT 100");

        List<SlaNotificationPendingPO> dueList = pendingMapper.selectList(wrapper);
        if (dueList.isEmpty()) {
            return 0;
        }

        int sentCount = 0;
        for (SlaNotificationPendingPO pending : dueList) {
            if (flushSinglePending(pending, now)) {
                sentCount++;
            }
        }
        return sentCount;
    }

    private void enqueuePending(Long ticketId,
                                NotificationType type,
                                String title,
                                String content,
                                String detailLink,
                                Collection<Long> receiverUserIds,
                                Collection<Long> mentionUserIds,
                                LocalDateTime triggerTime) {
        LambdaQueryWrapper<SlaNotificationPendingPO> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(SlaNotificationPendingPO::getTicketId, ticketId)
                .eq(SlaNotificationPendingPO::getNotificationType, type.getCode())
                .eq(SlaNotificationPendingPO::getStatus, SlaNotificationPendingStatus.PENDING.getCode());
        Long existingCount = pendingMapper.selectCount(existsWrapper);
        if (existingCount != null && existingCount > 0) {
            log.info("SLA通知已在延迟队列中，跳过重复入队: ticketId={}, type={}", ticketId, type.getCode());
            return;
        }

        Date scheduledAt = quietHours.resolveDeliveryDate(triggerTime);
        SlaNotificationPendingPO pending = new SlaNotificationPendingPO();
        pending.setTicketId(ticketId);
        pending.setNotificationType(type.getCode());
        pending.setTitle(title);
        pending.setContent(content);
        pending.setDetailLink(detailLink);
        pending.setReceiverUserIds(joinUserIds(receiverUserIds));
        pending.setMentionUserIds(joinUserIds(mentionUserIds));
        pending.setScheduledSendAt(scheduledAt);
        pending.setStatus(SlaNotificationPendingStatus.PENDING.getCode());
        pendingMapper.insert(pending);

        log.info("SLA通知已进入静默延迟队列: ticketId={}, type={}, scheduledSendAt={}",
                ticketId, type.getCode(), scheduledAt);
    }

    private boolean flushSinglePending(SlaNotificationPendingPO pending, Date sentAt) {
        try {
            NotificationType type = NotificationType.fromCode(pending.getNotificationType());
            if (type == null) {
                log.warn("未知的延迟通知类型，标记失败: pendingId={}, type={}",
                        pending.getId(), pending.getNotificationType());
                markPendingStatus(pending.getId(), SlaNotificationPendingStatus.FAILED.getCode(), null);
                return false;
            }
            sendImmediately(
                    pending.getTicketId(),
                    type,
                    pending.getTitle(),
                    pending.getContent(),
                    pending.getDetailLink(),
                    parseUserIds(pending.getReceiverUserIds()),
                    parseUserIds(pending.getMentionUserIds())
            );
            markPendingStatus(pending.getId(), SlaNotificationPendingStatus.SENT.getCode(), sentAt);
            return true;
        } catch (Exception e) {
            log.error("延迟SLA通知发送失败: pendingId={}, ticketId={}, type={}",
                    pending.getId(), pending.getTicketId(), pending.getNotificationType(), e);
            markPendingStatus(pending.getId(), SlaNotificationPendingStatus.FAILED.getCode(), null);
            return false;
        }
    }

    private void markPendingStatus(Long pendingId, String status, Date sentAt) {
        LambdaUpdateWrapper<SlaNotificationPendingPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SlaNotificationPendingPO::getId, pendingId)
                .set(SlaNotificationPendingPO::getStatus, status);
        if (sentAt != null) {
            updateWrapper.set(SlaNotificationPendingPO::getSentAt, sentAt);
        }
        pendingMapper.update(null, updateWrapper);
    }

    private void sendImmediately(Long ticketId,
                                 NotificationType type,
                                 String title,
                                 String content,
                                 String detailLink,
                                 Collection<Long> receiverUserIds,
                                 Collection<Long> mentionUserIds) {
        List<Long> receivers = receiverUserIds != null
                ? new ArrayList<>(receiverUserIds) : Collections.emptyList();
        if (!receivers.isEmpty()) {
            orchestrator.dispatchToUsers(receivers, ticketId, null, type, title, content, detailLink);
        }

        if (type == NotificationType.SLA_WARNING || type == NotificationType.SLA_BREACHED) {
            Set<Long> mentions = mentionUserIds != null
                    ? new LinkedHashSet<>(mentionUserIds) : new LinkedHashSet<>();
            wecomGroupPushService.pushByTicketWithUserMentions(ticketId, title, content, mentions);
        }
    }

    private String joinUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }
        return userIds.stream()
                .filter(id -> id != null)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Long> parseUserIds(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                log.warn("延迟队列用户ID解析失败: {}", trimmed);
            }
        }
        return result;
    }
}
