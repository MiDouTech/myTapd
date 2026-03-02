package com.miduo.cloud.ticket.application.wecom;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.notification.sender.WecomGroupWebhookSender;
import com.miduo.cloud.ticket.application.wecom.model.WecomBotParseResult;
import com.miduo.cloud.ticket.common.enums.WecomBotMessageStatus;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomCallbackMessageDTO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomBotMessageLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomGroupBindingMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomBotMessageLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomGroupBindingPO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 企微回调消息业务处理器
 */
@Service
public class WecomMessageProcessor extends BaseApplicationService {

    private final WecomBotMessageParser parser;
    private final WecomBotCommandService commandService;
    private final WecomGroupBindingMapper groupBindingMapper;
    private final WecomBotMessageLogMapper botMessageLogMapper;
    private final WecomGroupWebhookSender groupWebhookSender;
    private final TicketCategoryMapper ticketCategoryMapper;

    public WecomMessageProcessor(WecomBotMessageParser parser,
                                 WecomBotCommandService commandService,
                                 WecomGroupBindingMapper groupBindingMapper,
                                 WecomBotMessageLogMapper botMessageLogMapper,
                                 WecomGroupWebhookSender groupWebhookSender,
                                 TicketCategoryMapper ticketCategoryMapper) {
        this.parser = parser;
        this.commandService = commandService;
        this.groupBindingMapper = groupBindingMapper;
        this.botMessageLogMapper = botMessageLogMapper;
        this.groupWebhookSender = groupWebhookSender;
        this.ticketCategoryMapper = ticketCategoryMapper;
    }

    /**
     * 处理回调消息
     */
    public void process(WecomCallbackMessageDTO message) {
        if (message == null) {
            return;
        }

        if (isDuplicate(message)) {
            saveLog(message, null, null, WecomBotMessageStatus.DUPLICATE, "重复消息");
            return;
        }

        WecomGroupBindingPO binding = findActiveBinding(message.getChatId());
        String defaultCategoryPath = resolveDefaultCategoryPath(binding);

        try {
            String content = message.getContent() == null ? "" : message.getContent();
            WecomBotParseResult parseResult = parser.parse(content, defaultCategoryPath);
            WecomBotCommandService.CommandHandleResult commandResult = commandService.handle(
                    parseResult,
                    message.getChatId(),
                    message.getFromWecomUserid(),
                    binding != null ? binding.getDefaultCategoryId() : null
            );

            String reply = commandResult.getReplyContent();
            if (binding != null && binding.getWebhookUrl() != null && !binding.getWebhookUrl().trim().isEmpty()
                    && reply != null && !reply.trim().isEmpty()) {
                groupWebhookSender.sendToWebhook(binding.getWebhookUrl(), "工单助手", reply);
            }

            WecomBotMessageStatus status = parseResult.isSuccess()
                    ? WecomBotMessageStatus.SUCCESS
                    : WecomBotMessageStatus.FAIL;
            saveLog(message, parseResult, commandResult.getTicketId(), status,
                    parseResult.isSuccess() ? null : parseResult.getErrorMessage());
        } catch (Exception ex) {
            log.error("企微消息处理失败: msgId={}, chatId={}", message.getMsgId(), message.getChatId(), ex);
            saveLog(message, null, null, WecomBotMessageStatus.FAIL, ex.getMessage());
        }
    }

    private boolean isDuplicate(WecomCallbackMessageDTO message) {
        String msgId = message.getMsgId();
        if (msgId == null || msgId.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<WecomBotMessageLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WecomBotMessageLogPO::getMsgId, msgId.trim())
                .eq(WecomBotMessageLogPO::getChatId, message.getChatId())
                .last("LIMIT 1");
        return botMessageLogMapper.selectOne(wrapper) != null;
    }

    private WecomGroupBindingPO findActiveBinding(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<WecomGroupBindingPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WecomGroupBindingPO::getChatId, chatId.trim())
                .eq(WecomGroupBindingPO::getIsActive, 1)
                .last("LIMIT 1");
        return groupBindingMapper.selectOne(wrapper);
    }

    private String resolveDefaultCategoryPath(WecomGroupBindingPO binding) {
        if (binding == null || binding.getDefaultCategoryId() == null) {
            return null;
        }
        TicketCategoryPO defaultCategory = ticketCategoryMapper.selectById(binding.getDefaultCategoryId());
        if (defaultCategory == null) {
            return null;
        }
        List<TicketCategoryPO> categories = ticketCategoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategoryPO>()
                        .eq(TicketCategoryPO::getIsActive, 1)
        );
        if (categories == null || categories.isEmpty()) {
            return defaultCategory.getName();
        }
        Map<Long, TicketCategoryPO> categoryMap = categories.stream()
                .collect(Collectors.toMap(TicketCategoryPO::getId, c -> c));
        List<String> names = new ArrayList<>();
        TicketCategoryPO current = defaultCategory;
        while (current != null) {
            names.add(0, current.getName());
            current = current.getParentId() == null ? null : categoryMap.get(current.getParentId());
        }
        return String.join("/", names);
    }

    private void saveLog(WecomCallbackMessageDTO message,
                         WecomBotParseResult parseResult,
                         Long ticketId,
                         WecomBotMessageStatus status,
                         String errorMsg) {
        WecomBotMessageLogPO logPO = new WecomBotMessageLogPO();
        logPO.setChatId(message.getChatId());
        logPO.setMsgId(message.getMsgId());
        logPO.setFromWecomUserid(message.getFromWecomUserid());
        logPO.setRawMessage(message.getRawXml() == null ? message.getContent() : message.getRawXml());
        logPO.setParsedResult(parseResult == null ? null : JSON.toJSONString(parseResult));
        logPO.setTicketId(ticketId);
        logPO.setStatus(status.getCode());
        logPO.setErrorMsg(errorMsg);
        botMessageLogMapper.insert(logPO);
    }
}
