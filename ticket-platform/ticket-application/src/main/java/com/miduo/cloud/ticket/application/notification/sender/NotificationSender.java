package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;

/**
 * 通知发送器接口
 */
public interface NotificationSender {

    /**
     * 获取发送器对应的通知渠道
     */
    NotificationChannel getChannel();

    /**
     * 发送通知
     *
     * @param userId  目标用户ID
     * @param title   通知标题
     * @param content 通知内容
     */
    void send(Long userId, String title, String content);

    /**
     * 发送通知（可选业务详情链接，供企微卡片/邮件正文使用）
     */
    default void send(Long userId, String title, String content, String detailLink) {
        send(userId, title, content);
    }
}
