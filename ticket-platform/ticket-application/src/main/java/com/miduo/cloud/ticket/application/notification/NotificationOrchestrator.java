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
        if (userId == null) {
            log.warn("通知目标用户ID为空，跳过分发");
            return;
        }

        if (shouldAggregate(ticketId, type)) {
            log.info("通知已合并，跳过重复发送: userId={}, ticketId={}, type={}",
                    userId, ticketId, type.getCode());
            return;
        }

        NotificationPreferencePO preference = getUserPreference(userId, type.getCode());

        if (isSiteEnabled(preference)) {
            siteNotificationSender.send(userId, ticketId, reportId,
                    type.getCode(), title, content);
        }

        if (isWecomEnabled(preference)) {
            sendByChannel(NotificationChannel.WECOM_APP, userId, title, content);
        }

        if (isEmailEnabled(preference)) {
            sendByChannel(NotificationChannel.EMAIL, userId, title, content);
        }

        log.info("通知分发完成: userId={}, type={}, ticketId={}", userId, type.getCode(), ticketId);
    }

    /**
     * 批量分发通知到多个用户
     */
    public void dispatchToUsers(List<Long> userIds, Long ticketId, Long reportId,
                                NotificationType type, String title, String content) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            dispatch(userId, ticketId, reportId, type, title, content);
        }
    }

    /**
     * 消息合并：同一工单N分钟内的多次变更合并为一条通知
     */
    private boolean shouldAggregate(Long ticketId, NotificationType type) {
        if (ticketId == null) {
            return false;
        }

        int aggregateMinutes = getAggregateMinutes();
        if (aggregateMinutes <= 0) {
            return false;
        }

        String key = RedisKeyConstants.NOTIFY_AGGREGATE_PREFIX + ticketId + ":" + type.getCode();
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
                               String title, String content) {
        Map<NotificationChannel, NotificationSender> senderMap = senders.stream()
                .collect(Collectors.toMap(NotificationSender::getChannel, s -> s));
        NotificationSender sender = senderMap.get(channel);
        if (sender != null) {
            sender.send(userId, title, content);
        }
    }
}
