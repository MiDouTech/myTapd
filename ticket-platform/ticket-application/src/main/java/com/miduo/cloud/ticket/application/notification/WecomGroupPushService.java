package com.miduo.cloud.ticket.application.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.notification.sender.WecomGroupWebhookSender;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomGroupBindingMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomGroupBindingPO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 企微群Webhook推送服务
 */
@Service
public class WecomGroupPushService extends BaseApplicationService {

    private final TicketMapper ticketMapper;
    private final WecomGroupBindingMapper groupBindingMapper;
    private final WecomGroupWebhookSender groupWebhookSender;
    private final SysUserMapper sysUserMapper;

    public WecomGroupPushService(TicketMapper ticketMapper,
                                 WecomGroupBindingMapper groupBindingMapper,
                                 WecomGroupWebhookSender groupWebhookSender,
                                 SysUserMapper sysUserMapper) {
        this.ticketMapper = ticketMapper;
        this.groupBindingMapper = groupBindingMapper;
        this.groupWebhookSender = groupWebhookSender;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 按工单关联关系推送群消息（默认 @ 创建人）
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
        String creatorWecomUserid = resolveCreatorWecomUserid(ticket.getCreatorId());
        List<String> mention = creatorWecomUserid != null
                ? Collections.singletonList(creatorWecomUserid) : null;
        pushByTicketInternal(ticket, title, content, mention);
    }

    /**
     * 按工单推送群消息并 @ 指定系统用户（有企微 userid 的才会出现在 @ 列表）
     */
    public void pushByTicketWithUserMentions(Long ticketId, String title, String content,
                                             Collection<Long> userIdsToMention) {
        if (ticketId == null) {
            log.debug("企微群推送跳过：ticketId为空");
            return;
        }
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            log.warn("企微群推送跳过：工单不存在, ticketId={}", ticketId);
            return;
        }
        List<String> wecomIds = resolveWecomUserids(userIdsToMention);
        pushByTicketInternal(ticket, title, content, wecomIds.isEmpty() ? null : wecomIds);
    }

    private void pushByTicketInternal(TicketPO ticket, String title, String content,
                                      List<String> mentionedWecomUserIds) {
        Long ticketId = ticket.getId();
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
                groupWebhookSender.sendToWebhookWithMention(webhookUrl, title, content, mentionedWecomUserIds);
            } catch (Exception ex) {
                log.error("企微群推送失败: ticketId={}, bindingId={}, chatId={}, webhook={}, reason={}",
                        ticketId, binding.getId(), binding.getChatId(), sanitizeWebhookUrl(webhookUrl), ex.getMessage(), ex);
            }
        }
    }

    private List<String> resolveWecomUserids(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Long id : userIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(new ArrayList<>(ids));
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (SysUserPO u : users) {
            if (u == null || u.getWecomUserid() == null) {
                continue;
            }
            String w = u.getWecomUserid().trim();
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    /**
     * 根据多个关联工单的群绑定关系，发送预格式化的 Bug 简报归档通知（含@mention）
     * 正文直接使用已组装的 markdownBody，不额外包装
     *
     * @param ticketIds             关联的工单ID列表
     * @param markdownBody          已组装的Markdown正文（含标题行）
     * @param mentionedWecomUserIds 需要@的企微userId列表
     */
    public void pushReportNoticeByTickets(List<Long> ticketIds, String markdownBody,
                                          List<String> mentionedWecomUserIds) {
        if (CollectionUtils.isEmpty(ticketIds)) {
            log.debug("Bug简报归档群通知跳过：ticketIds为空");
            return;
        }
        List<TicketPO> tickets = ticketMapper.selectBatchIds(ticketIds);
        if (CollectionUtils.isEmpty(tickets)) {
            log.warn("Bug简报归档群通知跳过：未查到工单数据, ticketIds={}", ticketIds);
            return;
        }
        Set<String> pushedWebhookUrls = new HashSet<>();
        for (TicketPO ticket : tickets) {
            List<WecomGroupBindingPO> bindings = findRelatedBindings(ticket.getSourceChatId(), ticket.getCategoryId());
            for (WecomGroupBindingPO binding : bindings) {
                String webhookUrl = binding.getWebhookUrl();
                if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                    continue;
                }
                if (!pushedWebhookUrls.add(webhookUrl)) {
                    continue;
                }
                try {
                    log.info("Bug简报归档群通知发送中: ticketId={}, bindingId={}, webhook={}",
                            ticket.getId(), binding.getId(), sanitizeWebhookUrl(webhookUrl));
                    groupWebhookSender.sendReportNoticeToWebhook(webhookUrl, markdownBody, mentionedWecomUserIds);
                } catch (Exception ex) {
                    log.error("Bug简报归档群通知失败: ticketId={}, bindingId={}, webhook={}, reason={}",
                            ticket.getId(), binding.getId(), sanitizeWebhookUrl(webhookUrl), ex.getMessage(), ex);
                }
            }
        }
        if (pushedWebhookUrls.isEmpty()) {
            log.debug("Bug简报归档群通知跳过：关联工单均未绑定群, ticketIds={}", ticketIds);
        }
    }

    /**
     * 根据多个关联工单的群绑定关系，推送 @mention 群消息
     * 适用于 Bug 简报归档后通知工单创建人和处理人
     *
     * @param ticketIds             关联的工单ID列表
     * @param title                 消息标题
     * @param content               消息正文
     * @param mentionedWecomUserIds 需要@的企微userId列表
     */
    public void pushByTicketsWithMention(List<Long> ticketIds, String title, String content,
                                         List<String> mentionedWecomUserIds) {
        if (CollectionUtils.isEmpty(ticketIds)) {
            log.debug("企微群@mention推送跳过：ticketIds为空");
            return;
        }

        List<TicketPO> tickets = ticketMapper.selectBatchIds(ticketIds);
        if (CollectionUtils.isEmpty(tickets)) {
            log.warn("企微群@mention推送跳过：未查到工单数据, ticketIds={}", ticketIds);
            return;
        }

        Set<String> pushedWebhookUrls = new HashSet<>();
        for (TicketPO ticket : tickets) {
            List<WecomGroupBindingPO> bindings = findRelatedBindings(ticket.getSourceChatId(), ticket.getCategoryId());
            for (WecomGroupBindingPO binding : bindings) {
                String webhookUrl = binding.getWebhookUrl();
                if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                    continue;
                }
                if (!pushedWebhookUrls.add(webhookUrl)) {
                    continue;
                }
                try {
                    log.info("企微群@mention推送发送中: ticketId={}, bindingId={}, chatId={}, webhook={}",
                            ticket.getId(), binding.getId(), binding.getChatId(), sanitizeWebhookUrl(webhookUrl));
                    groupWebhookSender.sendToWebhookWithMention(webhookUrl, title, content, mentionedWecomUserIds);
                } catch (Exception ex) {
                    log.error("企微群@mention推送失败: ticketId={}, bindingId={}, webhook={}, reason={}",
                            ticket.getId(), binding.getId(), sanitizeWebhookUrl(webhookUrl), ex.getMessage(), ex);
                }
            }
        }

        if (pushedWebhookUrls.isEmpty()) {
            log.debug("企微群@mention推送跳过：关联工单均未绑定群, ticketIds={}", ticketIds);
        }
    }

    private String resolveCreatorWecomUserid(Long creatorId) {
        if (creatorId == null) {
            return null;
        }
        SysUserPO creator = sysUserMapper.selectById(creatorId);
        if (creator == null) {
            return null;
        }
        String wecomUserid = creator.getWecomUserid();
        return (wecomUserid != null && !wecomUserid.trim().isEmpty()) ? wecomUserid.trim() : null;
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
