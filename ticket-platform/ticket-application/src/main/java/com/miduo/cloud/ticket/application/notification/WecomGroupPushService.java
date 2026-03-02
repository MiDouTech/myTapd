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
            return;
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            return;
        }
        List<WecomGroupBindingPO> bindings = findRelatedBindings(ticket.getSourceChatId(), ticket.getCategoryId());
        if (bindings.isEmpty()) {
            return;
        }

        Set<String> pushedWebhookUrls = new HashSet<>();
        for (WecomGroupBindingPO binding : bindings) {
            String webhookUrl = binding.getWebhookUrl();
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                continue;
            }
            if (!pushedWebhookUrls.add(webhookUrl)) {
                continue;
            }
            groupWebhookSender.sendToWebhook(webhookUrl, title, content);
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
}
