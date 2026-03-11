package com.miduo.cloud.ticket.application.wecom;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.notification.sender.WecomGroupWebhookSender;
import com.miduo.cloud.ticket.application.wecom.model.WecomBotParseResult;
import com.miduo.cloud.ticket.application.wecom.model.WecomDraftSession;
import com.miduo.cloud.ticket.common.enums.WecomBotCommandType;
import com.miduo.cloud.ticket.common.enums.WecomBotMessageStatus;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpAnalyzeResult;
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

    private static final String PARSE_TYPE_TEMPLATE = "template";
    private static final String PARSE_TYPE_NATURAL_LANGUAGE = "natural_language";

    private final WecomBotMessageParser parser;
    private final WecomBotCommandService commandService;
    private final WecomGroupBindingMapper groupBindingMapper;
    private final WecomBotMessageLogMapper botMessageLogMapper;
    private final WecomGroupWebhookSender groupWebhookSender;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final WecomDraftSessionService draftSessionService;
    private final WecomNaturalLangParser naturalLangParser;
    private final WecomInteractiveConfirmService interactiveConfirmService;

    public WecomMessageProcessor(WecomBotMessageParser parser,
                                 WecomBotCommandService commandService,
                                 WecomGroupBindingMapper groupBindingMapper,
                                 WecomBotMessageLogMapper botMessageLogMapper,
                                 WecomGroupWebhookSender groupWebhookSender,
                                 TicketCategoryMapper ticketCategoryMapper,
                                 WecomDraftSessionService draftSessionService,
                                 WecomNaturalLangParser naturalLangParser,
                                 WecomInteractiveConfirmService interactiveConfirmService) {
        this.parser = parser;
        this.commandService = commandService;
        this.groupBindingMapper = groupBindingMapper;
        this.botMessageLogMapper = botMessageLogMapper;
        this.groupWebhookSender = groupWebhookSender;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.draftSessionService = draftSessionService;
        this.naturalLangParser = naturalLangParser;
        this.interactiveConfirmService = interactiveConfirmService;
    }

    /**
     * 处理回调消息
     */
    public void process(WecomCallbackMessageDTO message) {
        if (message == null) {
            return;
        }
        log.info("开始处理企微回调消息: msgId={}, chatId={}, fromWecomUserid={}",
                message.getMsgId(), message.getChatId(), message.getFromWecomUserid());

        if (isDuplicate(message)) {
            saveLog(message, null, null, WecomBotMessageStatus.DUPLICATE, "重复消息", null, null);
            log.info("企微回调消息重复，已记录日志并跳过: msgId={}, chatId={}", message.getMsgId(), message.getChatId());
            return;
        }

        WecomGroupBindingPO binding = findActiveBinding(message.getChatId());
        String defaultCategoryPath = resolveDefaultCategoryPath(binding);
        String chatId = message.getChatId();
        String fromWecomUserId = message.getFromWecomUserid();

        try {
            String content = message.getContent() == null ? "" : message.getContent();

            WecomDraftSession existingDraft = draftSessionService.getDraft(chatId, fromWecomUserId);
            if (existingDraft != null) {
                String reply = interactiveConfirmService.handleReply(content, existingDraft, chatId, fromWecomUserId);
                sendReply(binding, reply);
                saveLog(message, null, null, WecomBotMessageStatus.SUCCESS, null,
                        PARSE_TYPE_NATURAL_LANGUAGE, null);
                log.info("企微消息进入草稿确认流程: msgId={}, chatId={}", message.getMsgId(), chatId);
                return;
            }

            WecomBotParseResult parseResult = parser.parse(content, defaultCategoryPath);

            if (parseResult.isSuccess() && WecomBotCommandType.NATURAL_LANGUAGE == parseResult.getCommandType()) {
                handleNaturalLanguageMessage(message, parseResult, binding, defaultCategoryPath);
                return;
            }

            WecomBotCommandService.CommandHandleResult commandResult = commandService.handle(
                    parseResult,
                    chatId,
                    fromWecomUserId,
                    binding != null ? binding.getDefaultCategoryId() : null
            );

            sendReply(binding, commandResult.getReplyContent());

            WecomBotMessageStatus status = parseResult.isSuccess()
                    ? WecomBotMessageStatus.SUCCESS
                    : WecomBotMessageStatus.FAIL;
            saveLog(message, parseResult, commandResult.getTicketId(), status,
                    parseResult.isSuccess() ? null : parseResult.getErrorMessage(),
                    PARSE_TYPE_TEMPLATE, null);
            log.info("企微回调消息处理完成: msgId={}, chatId={}, status={}, ticketId={}",
                    message.getMsgId(), chatId, status.getCode(), commandResult.getTicketId());
        } catch (Exception ex) {
            log.error("企微消息处理失败: msgId={}, chatId={}", message.getMsgId(), chatId, ex);
            saveLog(message, null, null, WecomBotMessageStatus.FAIL, ex.getMessage(), null, null);
        }
    }

    private void handleNaturalLanguageMessage(WecomCallbackMessageDTO message,
                                               WecomBotParseResult parseResult,
                                               WecomGroupBindingPO binding,
                                               String defaultCategoryPath) {
        String rawText = parseResult.getRawNaturalLanguageText();
        String chatId = message.getChatId();
        String fromWecomUserId = message.getFromWecomUserid();

        NlpAnalyzeResult nlpResult = naturalLangParser.analyze(rawText, defaultCategoryPath);

        WecomDraftSession draft = new WecomDraftSession();
        draft.setSessionKey(chatId != null ? chatId : fromWecomUserId);
        draft.setWecomUserId(fromWecomUserId);
        draft.setStep(WecomDraftSession.Step.PENDING_CONFIRM);
        draft.setTitle(nlpResult.getTitle());
        draft.setCategoryPath(nlpResult.getCategoryPath());
        draft.setPriority(nlpResult.getPriority());
        draft.setDescription(rawText);
        draft.setNlpConfidence(nlpResult.getConfidence());
        draft.setChatId(chatId);
        boolean isGroup = chatId != null && !chatId.trim().isEmpty();
        draft.setGroupChat(isGroup);

        draftSessionService.saveDraft(chatId != null ? chatId : fromWecomUserId, fromWecomUserId, draft, isGroup);

        String previewMessage = interactiveConfirmService.buildDraftPreviewMessage(draft);
        sendReply(binding, previewMessage);

        saveLog(message, parseResult, null, WecomBotMessageStatus.SUCCESS, null,
                PARSE_TYPE_NATURAL_LANGUAGE, nlpResult.getConfidence() != null ? nlpResult.getConfidence().byteValue() : null);
        log.info("企微自然语言消息已解析并生成草稿: msgId={}, chatId={}, confidence={}",
                message.getMsgId(), chatId, nlpResult.getConfidence());
    }

    private void sendReply(WecomGroupBindingPO binding, String reply) {
        if (binding != null && binding.getWebhookUrl() != null && !binding.getWebhookUrl().trim().isEmpty()
                && reply != null && !reply.trim().isEmpty()) {
            groupWebhookSender.sendToWebhook(binding.getWebhookUrl(), "工单助手", reply);
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
                         String errorMsg,
                         String parseType,
                         Byte nlpConfidence) {
        WecomBotMessageLogPO logPO = new WecomBotMessageLogPO();
        logPO.setChatId(message.getChatId());
        logPO.setMsgId(message.getMsgId());
        logPO.setFromWecomUserid(message.getFromWecomUserid());
        logPO.setRawMessage(message.getRawXml() == null ? message.getContent() : message.getRawXml());
        logPO.setParsedResult(parseResult == null ? null : JSON.toJSONString(parseResult));
        logPO.setTicketId(ticketId);
        logPO.setStatus(status.getCode());
        logPO.setErrorMsg(errorMsg);
        logPO.setParseType(parseType);
        logPO.setNlpConfidence(nlpConfidence != null ? nlpConfidence.intValue() : null);
        botMessageLogMapper.insert(logPO);
    }
}
