package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.notification.sender.WecomGroupWebhookSender;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomGroupBindingMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomGroupBindingPO;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 企微群Webhook推送服务
 */
@Service
public class WecomGroupPushService extends BaseApplicationService {

    private final TicketMapper ticketMapper;
    private final WecomGroupBindingMapper groupBindingMapper;
    private final WecomGroupWebhookSender groupWebhookSender;

    public WecomGroupPushService(TicketMapper ticketMapper,
                                 WecomGroupBindingMapper groupBindingMapper,
                                 WecomGroupWebhookSender groupWebhookSender) {
        this.ticketMapper = ticketMapper;
        this.groupBindingMapper = groupBindingMapper;
        this.groupWebhookSender = groupWebhookSender;
    }

    /**
     * 按工单关联关系推送群消息
     */
    public void pushByTicket(Long ticketId, String title, String content) {
        if (ticketId == null) {
            log.debug("企微群推送跳过：ticketId为空");
            return;
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            log.warn("企微群推送跳过：工单不存在, ticketId={}", ticketId);
            return;
        }
        log.debug("开始企微群推送: ticketId={}, sourceChatId={}, categoryId={}",
                ticketId, ticket.getSourceChatId(), ticket.getCategoryId());
        List<WecomGroupBindingPO> bindings = findRelatedBindings(ticket.getSourceChatId(), ticket.getCategoryId());
        if (bindings.isEmpty()) {
            log.debug("企微群推送跳过：未找到匹配群绑定, ticketId={}, sourceChatId={}, categoryId={}",
                    ticketId, ticket.getSourceChatId(), ticket.getCategoryId());
            return;
        }

        Set<String> pushedWebhookUrls = new HashSet<>();
        for (WecomGroupBindingPO binding : bindings) {
            String webhookUrl = binding.getWebhookUrl();
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                log.debug("企微群推送跳过：绑定缺少Webhook地址, bindingId={}, chatId={}",
                        binding.getId(), binding.getChatId());
                continue;
            }
            if (!pushedWebhookUrls.add(webhookUrl)) {
                log.debug("企微群推送去重：重复Webhook地址已跳过, bindingId={}, chatId={}",
                        binding.getId(), binding.getChatId());
                continue;
            }
            try {
                log.info("企微群推送发送中: ticketId={}, bindingId={}, chatId={}, webhook={}",
                        ticketId, binding.getId(), binding.getChatId(), sanitizeWebhookUrl(webhookUrl));
                groupWebhookSender.sendToWebhook(webhookUrl, title, content);
            } catch (Exception ex) {
                log.error("企微群推送失败: ticketId={}, bindingId={}, chatId={}, webhook={}, reason={}",
                        ticketId, binding.getId(), binding.getChatId(), sanitizeWebhookUrl(webhookUrl), ex.getMessage(), ex);
            }
        }
    }

    private List<WecomGroupBindingPO> findRelatedBindings(String sourceChatId, Long categoryId) {
        if ((sourceChatId == null || sourceChatId.trim().isEmpty()) && categoryId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<WecomGroupBindingPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WecomGroupBindingPO::getIsActive, 1);

        boolean hasChatId = sourceChatId != null && !sourceChatId.trim().isEmpty();
        boolean hasCategory = categoryId != null;
        if (hasChatId && hasCategory) {
            wrapper.and(w -> w.eq(WecomGroupBindingPO::getChatId, sourceChatId.trim())
                    .or()
                    .eq(WecomGroupBindingPO::getDefaultCategoryId, categoryId));
        } else if (hasChatId) {
            wrapper.eq(WecomGroupBindingPO::getChatId, sourceChatId.trim());
        } else {
            wrapper.eq(WecomGroupBindingPO::getDefaultCategoryId, categoryId);
        }
        return groupBindingMapper.selectList(wrapper);
    }

    private String sanitizeWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            return "";
        }
        String normalized = webhookUrl.trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            return normalized.substring(0, queryIndex) + "?***";
        }
        return normalized;
    }
}
