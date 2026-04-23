package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.notification.sender.NotificationSender;
import com.miduo.cloud.ticket.application.notification.sender.SiteNotificationSender;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import com.miduo.cloud.ticket.common.enums.NotificationChannel;
import com.miduo.cloud.ticket.common.enums.NotificationType;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper.NotificationPreferenceMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po.NotificationPreferencePO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 通知编排器
 * 根据事件类型、用户偏好和消息合并策略分发通知到各渠道
 */
@Component
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);

    private final List<NotificationSender> senders;
    private final SiteNotificationSender siteNotificationSender;
    private final NotificationPreferenceMapper preferenceMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final StringRedisTemplate redisTemplate;

    public NotificationOrchestrator(List<NotificationSender> senders,
                                    SiteNotificationSender siteNotificationSender,
                                    NotificationPreferenceMapper preferenceMapper,
                                    SystemConfigMapper systemConfigMapper,
                                    StringRedisTemplate redisTemplate) {
        this.senders = senders;
        this.siteNotificationSender = siteNotificationSender;
        this.preferenceMapper = preferenceMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 分发通知到各渠道
     *
     * @param userId   目标用户ID
     * @param ticketId 关联工单ID（可为空）
     * @param reportId 关联简报ID（可为空）
     * @param type     通知类型
     * @param title    通知标题
     * @param content  通知内容
     */
    public void dispatch(Long userId, Long ticketId, Long reportId,
                         NotificationType type, String title, String content) {
        dispatch(userId, ticketId, reportId, type, title, content, null);
    }

    /**
     * @param detailLink 工单等业务详情 URL（可空）；评论 @ 等场景传入后企微卡片与邮件可直达详情
     */
    public void dispatch(Long userId, Long ticketId, Long reportId,
                         NotificationType type, String title, String content, String detailLink) {
        if (userId == null) {
            log.warn("通知目标用户ID为空，跳过分发");
            return;
        }

        if (shouldAggregate(userId, ticketId, type)) {
            log.info("通知已合并，跳过重复发送: userId={}, ticketId={}, type={}",
                    userId, ticketId, type.getCode());
            return;
        }

        NotificationPreferencePO preference = getUserPreference(userId, type.getCode());

        boolean siteOn = isSiteEnabled(preference);
        boolean wecomOn = isWecomEnabled(preference);
        boolean emailOn = isEmailEnabled(preference);

        // 简报提审：审核人必须收到提醒；若用户曾把该类型「站内+企微+邮件」全部关掉，则仍发站内信，避免漏审
        if (!siteOn && !wecomOn && !emailOn && type == NotificationType.REPORT_SUBMITTED) {
            log.warn("简报提审通知全部渠道已关闭，已强制发送站内信: userId={}", userId);
            siteNotificationSender.send(userId, ticketId, reportId, type.getCode(), title, content);
            log.info("通知分发完成: userId={}, type={}, ticketId={}", userId, type.getCode(), ticketId);
            return;
        }

        if (siteOn) {
            siteNotificationSender.send(userId, ticketId, reportId,
                    type.getCode(), title, content);
        }

        if (wecomOn) {
            sendByChannel(NotificationChannel.WECOM_APP, userId, title, content, detailLink);
        }

        if (emailOn) {
            sendByChannel(NotificationChannel.EMAIL, userId, title, content, detailLink);
        }

        log.info("通知分发完成: userId={}, type={}, ticketId={}", userId, type.getCode(), ticketId);
    }

    /**
     * 批量分发通知到多个用户
     */
    public void dispatchToUsers(List<Long> userIds, Long ticketId, Long reportId,
                                NotificationType type, String title, String content) {
        dispatchToUsers(userIds, ticketId, reportId, type, title, content, null);
    }

    public void dispatchToUsers(List<Long> userIds, Long ticketId, Long reportId,
                                NotificationType type, String title, String content, String detailLink) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            dispatch(userId, ticketId, reportId, type, title, content, detailLink);
        }
    }

    /**
     * 消息合并：同一用户在N分钟内不重复接收同一工单的同类型通知
     * Key 维度为 userId:ticketId:type，避免多接收人批量发送时误合并
     */
    private boolean shouldAggregate(Long userId, Long ticketId, NotificationType type) {
        if (ticketId == null || userId == null) {
            return false;
        }
        // 评论 @ 提醒需逐条送达，避免 N 分钟内多人 @ 同一人时被合并丢失
        if (type == NotificationType.COMMENT_MENTION) {
            return false;
        }

        int aggregateMinutes = getAggregateMinutes();
        if (aggregateMinutes <= 0) {
            return false;
        }

        String key = RedisKeyConstants.NOTIFY_AGGREGATE_PREFIX + userId + ":" + ticketId + ":" + type.getCode();
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }

        redisTemplate.opsForValue().set(key, "1", aggregateMinutes, TimeUnit.MINUTES);
        return false;
    }

    private int getAggregateMinutes() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigKey, "notification_aggregate_minutes");
        SystemConfigPO config = systemConfigMapper.selectOne(wrapper);
        if (config != null && config.getConfigValue() != null) {
            try {
                return Integer.parseInt(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("通知合并时间配置解析失败: {}", config.getConfigValue());
            }
        }
        return 5;
    }

    private NotificationPreferencePO getUserPreference(Long userId, String eventType) {
        LambdaQueryWrapper<NotificationPreferencePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationPreferencePO::getUserId, userId)
                .eq(NotificationPreferencePO::getEventType, eventType);
        return preferenceMapper.selectOne(wrapper);
    }

    private boolean isSiteEnabled(NotificationPreferencePO pref) {
        return pref == null || pref.getSiteEnabled() == null || pref.getSiteEnabled() == 1;
    }

    private boolean isWecomEnabled(NotificationPreferencePO pref) {
        return pref == null || pref.getWecomEnabled() == null || pref.getWecomEnabled() == 1;
    }

    private boolean isEmailEnabled(NotificationPreferencePO pref) {
        return pref != null && pref.getEmailEnabled() != null && pref.getEmailEnabled() == 1;
    }

    private void sendByChannel(NotificationChannel channel, Long userId,
                               String title, String content, String detailLink) {
        Map<NotificationChannel, NotificationSender> senderMap = senders.stream()
                .collect(Collectors.toMap(NotificationSender::getChannel, s -> s));
        NotificationSender sender = senderMap.get(channel);
        if (sender != null) {
            try {
                sender.send(userId, title, content, detailLink);
            } catch (Exception e) {
                log.warn("渠道发送失败，已跳过: channel={}, userId={}, error={}", channel, userId, e.getMessage());
            }
        }
    }
}
