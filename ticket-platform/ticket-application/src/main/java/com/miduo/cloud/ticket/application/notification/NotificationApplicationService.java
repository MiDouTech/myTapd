package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.NotificationChannel;
import com.miduo.cloud.ticket.common.enums.NotificationType;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.notification.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper.NotificationMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper.NotificationPreferenceMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po.NotificationPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po.NotificationPreferencePO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知应用服务
 * 处理通知列表查询、已读标记、偏好管理
 */
@Service
public class NotificationApplicationService extends BaseApplicationService {

    private final NotificationMapper notificationMapper;
    private final NotificationPreferenceMapper preferenceMapper;

    public NotificationApplicationService(NotificationMapper notificationMapper,
                                          NotificationPreferenceMapper preferenceMapper) {
        this.notificationMapper = notificationMapper;
        this.preferenceMapper = preferenceMapper;
    }

    /**
     * 分页查询通知列表
     */
    public PageOutput<NotificationOutput> pageNotifications(Long userId, NotificationPageInput input) {
        LambdaQueryWrapper<NotificationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationPO::getUserId, userId)
                .eq(NotificationPO::getChannel, NotificationChannel.SITE.getCode());

        if (input.getType() != null && !input.getType().isEmpty()) {
            wrapper.eq(NotificationPO::getType, input.getType());
        }
        if (input.getIsRead() != null) {
            wrapper.eq(NotificationPO::getIsRead, input.getIsRead());
        }
        if (StringUtils.hasText(input.getCreateTimeStart())) {
            wrapper.ge(NotificationPO::getCreateTime, input.getCreateTimeStart());
        }
        if (StringUtils.hasText(input.getCreateTimeEnd())) {
            wrapper.le(NotificationPO::getCreateTime, input.getCreateTimeEnd());
        }

        wrapper.orderByDesc(NotificationPO::getCreateTime);

        Page<NotificationPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        Page<NotificationPO> result = notificationMapper.selectPage(page, wrapper);

        List<NotificationOutput> records = result.getRecords().stream()
                .map(this::convertToOutput)
                .collect(Collectors.toList());

        return PageOutput.of(records, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    /**
     * 标记通知为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long notificationId) {
        NotificationPO notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "无权操作该通知");
        }
        if (notification.getIsRead() == 1) {
            return;
        }

        LambdaUpdateWrapper<NotificationPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NotificationPO::getId, notificationId)
                .set(NotificationPO::getIsRead, 1)
                .set(NotificationPO::getReadAt, new Date());
        notificationMapper.update(null, wrapper);

        log.info("通知已标记为已读: notificationId={}, userId={}", notificationId, userId);
    }

    /**
     * 批量标记所有未读通知为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<NotificationPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(NotificationPO::getUserId, userId)
                .eq(NotificationPO::getIsRead, 0)
                .set(NotificationPO::getIsRead, 1)
                .set(NotificationPO::getReadAt, new Date());
        notificationMapper.update(null, wrapper);

        log.info("所有未读通知已标记为已读: userId={}", userId);
    }

    /**
     * 查询未读通知数量
     */
    public NotificationUnreadCountOutput getUnreadCount(Long userId) {
        int count = notificationMapper.countUnreadByUserId(userId);
        return new NotificationUnreadCountOutput(count);
    }

    /**
     * 获取用户通知偏好列表
     */
    public List<NotificationPreferenceOutput> getPreferences(Long userId) {
        List<NotificationPreferencePO> existing = preferenceMapper.selectByUserId(userId);
        Map<String, NotificationPreferencePO> existingMap = existing.stream()
                .collect(Collectors.toMap(NotificationPreferencePO::getEventType, p -> p));

        List<NotificationPreferenceOutput> result = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            NotificationPreferencePO po = existingMap.get(type.getCode());
            NotificationPreferenceOutput output = new NotificationPreferenceOutput();
            output.setUserId(userId);
            output.setEventType(type.getCode());
            output.setEventTypeLabel(type.getLabel());

            if (po != null) {
                output.setId(po.getId());
                output.setSiteEnabled(po.getSiteEnabled());
                output.setWecomEnabled(po.getWecomEnabled());
                output.setEmailEnabled(po.getEmailEnabled());
            } else {
                output.setSiteEnabled(1);
                output.setWecomEnabled(1);
                // 评论 @ 默认同步邮件（需用户档案有邮箱且配置 spring.mail.*）
                output.setEmailEnabled(type == NotificationType.COMMENT_MENTION ? 1 : 0);
            }
            result.add(output);
        }
        return result;
    }

    /**
     * 更新用户通知偏好
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePreferences(Long userId, NotificationPreferenceUpdateInput input) {
        List<NotificationPreferencePO> existingList = preferenceMapper.selectByUserId(userId);
        Map<String, NotificationPreferencePO> existingMap = existingList.stream()
                .collect(Collectors.toMap(NotificationPreferencePO::getEventType, p -> p));

        for (NotificationPreferenceUpdateInput.PreferenceItem item : input.getItems()) {
            NotificationType type = NotificationType.fromCode(item.getEventType());
            if (type == null) {
                continue;
            }

            NotificationPreferencePO existing = existingMap.get(item.getEventType());
            if (existing != null) {
                if (item.getSiteEnabled() != null) {
                    existing.setSiteEnabled(item.getSiteEnabled());
                }
                if (item.getWecomEnabled() != null) {
                    existing.setWecomEnabled(item.getWecomEnabled());
                }
                if (item.getEmailEnabled() != null) {
                    existing.setEmailEnabled(item.getEmailEnabled());
                }
                preferenceMapper.updateById(existing);
            } else {
                NotificationPreferencePO newPref = new NotificationPreferencePO();
                newPref.setUserId(userId);
                newPref.setEventType(item.getEventType());
                newPref.setSiteEnabled(item.getSiteEnabled() != null ? item.getSiteEnabled() : 1);
                newPref.setWecomEnabled(item.getWecomEnabled() != null ? item.getWecomEnabled() : 1);
                newPref.setEmailEnabled(item.getEmailEnabled() != null ? item.getEmailEnabled() : 0);
                preferenceMapper.insert(newPref);
            }
        }

        log.info("用户通知偏好已更新: userId={}", userId);
    }

    private NotificationOutput convertToOutput(NotificationPO po) {
        if (po == null) {
            return null;
        }
        NotificationOutput output = new NotificationOutput();
        output.setId(po.getId());
        output.setUserId(po.getUserId());
        output.setTicketId(po.getTicketId());
        output.setReportId(po.getReportId());
        output.setType(po.getType());
        NotificationType type = NotificationType.fromCode(po.getType());
        output.setTypeLabel(type != null ? type.getLabel() : po.getType());
        output.setChannel(po.getChannel());
        NotificationChannel channel = NotificationChannel.fromCode(po.getChannel());
        output.setChannelLabel(channel != null ? channel.getLabel() : po.getChannel());
        output.setTitle(po.getTitle());
        output.setContent(po.getContent());
        output.setIsRead(po.getIsRead());
        output.setReadAt(po.getReadAt());
        output.setCreateTime(po.getCreateTime());
        return output;
    }
}
