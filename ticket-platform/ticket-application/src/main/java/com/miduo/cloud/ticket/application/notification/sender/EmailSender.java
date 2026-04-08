package com.miduo.cloud.ticket.application.notification.sender;

import com.miduo.cloud.ticket.common.enums.NotificationChannel;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮件通知发送器（依赖 spring.mail.* 配置；未配置 host 时跳过发送）
 */
@Component
public class EmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SysUserMapper sysUserMapper;

    @Value("${spring.mail.username:}")
    private String mailFromUsername;

    @Value("${spring.mail.properties.from:}")
    private String mailFromOverride;

    public EmailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                       SysUserMapper sysUserMapper) {
        this.mailSenderProvider = mailSenderProvider;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Long userId, String title, String content) {
        send(userId, title, content, null);
    }

    @Override
    public void send(Long userId, String title, String content, String detailLink) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.debug("未配置 spring.mail.host，跳过邮件发送: userId={}", userId);
            return;
        }
        if (userId == null) {
            log.warn("邮件通知目标用户为空，跳过发送");
            return;
        }
        SysUserPO user = sysUserMapper.selectById(userId);
        if (user == null) {
            log.warn("邮件通知目标用户不存在，跳过发送: userId={}", userId);
            return;
        }
        String to = user.getEmail();
        if (!StringUtils.hasText(to)) {
            log.warn("用户未配置邮箱，跳过邮件发送: userId={}", userId);
            return;
        }

        String from = resolveFromAddress();
        if (!StringUtils.hasText(from)) {
            log.warn("未配置发件人（spring.mail.username 或 spring.mail.properties.from），跳过邮件发送");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to.trim());
        message.setSubject(title != null ? title : "工单通知");
        message.setText(buildBody(content, detailLink));

        try {
            mailSender.send(message);
            log.info("邮件通知已发送: userId={}, to={}", userId, to);
        } catch (Exception e) {
            log.warn("邮件发送失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(mailFromOverride)) {
            return mailFromOverride.trim();
        }
        if (StringUtils.hasText(mailFromUsername)) {
            return mailFromUsername.trim();
        }
        return "";
    }

    private static String buildBody(String content, String detailLink) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(content)) {
            sb.append(content.trim());
        }
        if (StringUtils.hasText(detailLink)) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("详情链接：").append(detailLink.trim());
        }
        if (sb.length() == 0) {
            return "（无正文）";
        }
        return sb.toString();
    }
}
