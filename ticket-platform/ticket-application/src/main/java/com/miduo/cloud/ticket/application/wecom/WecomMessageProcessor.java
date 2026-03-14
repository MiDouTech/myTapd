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
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
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
    private final WecomClient wecomClient;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final TicketMapper ticketMapper;
    private final WecomNaturalLangParser naturalLangParser;
    private final WecomInteractiveConfirmService interactiveConfirmService;
    private final SysUserMapper sysUserMapper;
    private final WecomProperties wecomProperties;
    private final WecomImageHandlerService imageHandlerService;

    public WecomMessageProcessor(WecomBotMessageParser parser,
                                 WecomBotCommandService commandService,
                                 WecomGroupBindingMapper groupBindingMapper,
                                 WecomBotMessageLogMapper botMessageLogMapper,
                                 WecomGroupWebhookSender groupWebhookSender,
                                 WecomClient wecomClient,
                                 TicketCategoryMapper ticketCategoryMapper,
                                 TicketMapper ticketMapper,
                                 WecomNaturalLangParser naturalLangParser,
                                 WecomInteractiveConfirmService interactiveConfirmService,
                                 SysUserMapper sysUserMapper,
                                 WecomProperties wecomProperties,
                                 WecomImageHandlerService imageHandlerService) {
        this.parser = parser;
        this.commandService = commandService;
        this.groupBindingMapper = groupBindingMapper;
        this.botMessageLogMapper = botMessageLogMapper;
        this.groupWebhookSender = groupWebhookSender;
        this.wecomClient = wecomClient;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.ticketMapper = ticketMapper;
        this.naturalLangParser = naturalLangParser;
        this.interactiveConfirmService = interactiveConfirmService;
        this.sysUserMapper = sysUserMapper;
        this.wecomProperties = wecomProperties;
        this.imageHandlerService = imageHandlerService;
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
            String rawContent = message.getContent() == null ? "" : message.getContent();
            // 群聊中@机器人时内容格式为 "<@botid_xxx> 用户输入"，需统一提前去除@提及前缀
            String content = stripBotMention(rawContent);

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

            sendReply(binding, commandResult.getReplyContent(), fromWecomUserId, message.getResponseUrl());

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
        draft.setTitle(nlpResult.getTitle());
        draft.setCategoryPath(nlpResult.getCategoryPath());
        draft.setPriority(nlpResult.getPriority());
        draft.setDescription(rawText);
        draft.setNlpConfidence(nlpResult.getConfidence());
        draft.setChatId(chatId);

        Long ticketId = interactiveConfirmService.createTicketDirectly(draft, chatId, fromWecomUserId);
        if (ticketId != null) {
            int linkedImages = imageHandlerService.linkPendingImagesToTicket(ticketId, chatId, fromWecomUserId);
            TicketPO ticket = ticketMapper.selectById(ticketId);
            String ticketNo = ticket != null ? ticket.getTicketNo() : "";
            String replyMsg = buildTicketCreatedReply(ticketNo, draft.getTitle(), draft.getCategoryPath(),
                    draft.getPriority(), linkedImages);
            sendReply(binding, replyMsg, fromWecomUserId, message.getResponseUrl());
            saveLog(message, parseResult, ticketId, WecomBotMessageStatus.SUCCESS, null,
                    PARSE_TYPE_NATURAL_LANGUAGE, nlpResult.getConfidence() != null ? nlpResult.getConfidence().byteValue() : null);
            log.info("企微自然语言消息已直接创建工单: msgId={}, chatId={}, ticketId={}, confidence={}, linkedImages={}",
                    message.getMsgId(), chatId, ticketId, nlpResult.getConfidence(), linkedImages);
        } else {
            saveLog(message, parseResult, null, WecomBotMessageStatus.FAIL,
                    "未关联系统账号，无法创建工单",
                    PARSE_TYPE_NATURAL_LANGUAGE, nlpResult.getConfidence() != null ? nlpResult.getConfidence().byteValue() : null);
            log.warn("企微自然语言消息处理失败：发送人未关联系统账号: msgId={}, chatId={}, fromWecomUserId={}",
                    message.getMsgId(), chatId, fromWecomUserId);
        }
    }

    private String buildTicketCreatedReply(String ticketNo, String title, String categoryPath, String priority, int linkedImages) {
        String publicLink = buildPublicTicketLink(ticketNo);
        String priorityLabel = formatPriority(priority);
        StringBuilder sb = new StringBuilder("✅ 工单创建成功\n")
                .append("工单编号：").append(safeValue(ticketNo)).append("\n")
                .append("标题：").append(safeValue(title)).append("\n")
                .append("分类：").append(safeValue(categoryPath)).append("\n")
                .append("优先级：").append(priorityLabel).append("\n");
        if (linkedImages > 0) {
            sb.append("🖼️ 图片附件：").append(linkedImages).append("张（已上传）\n");
        }
        sb.append("查看详情：").append(publicLink);
        return sb.toString();
    }

    private String formatPriority(String priority) {
        if (priority == null) {
            return "中";
        }
        switch (priority) {
            case "urgent": return "紧急";
            case "high": return "高";
            case "low": return "低";
            default: return "中";
        }
    }

    private String safeValue(String value) {
        return value == null ? "-" : value;
    }

    private String buildPublicTicketLink(String ticketNo) {
        if (ticketNo == null || ticketNo.trim().isEmpty()) {
            return "-";
        }
        String domain = wecomProperties.getTrustedDomain();
        if (domain == null || domain.trim().isEmpty()) {
            return "-";
        }
        String normalizedDomain = domain.trim();
        if (!normalizedDomain.startsWith("http://") && !normalizedDomain.startsWith("https://")) {
            normalizedDomain = "https://" + normalizedDomain;
        }
        return normalizedDomain + "/open/ticket/" + ticketNo.trim();
    }

    private void sendReply(WecomGroupBindingPO binding, String reply) {
        if (reply == null || reply.trim().isEmpty()) {
            return;
        }
        if (binding != null && binding.getWebhookUrl() != null && !binding.getWebhookUrl().trim().isEmpty()) {
            groupWebhookSender.sendToWebhook(binding.getWebhookUrl(), "工单助手", reply);
        }
    }

    /**
     * 发送回复（支持单聊：优先使用response_url直接回复，兜底通过企微应用消息发送给个人）
     */
    private void sendReply(WecomGroupBindingPO binding, String reply, String fromWecomUserId, String responseUrl) {
        if (reply == null || reply.trim().isEmpty()) {
            return;
        }
        if (binding != null && binding.getWebhookUrl() != null && !binding.getWebhookUrl().trim().isEmpty()) {
            groupWebhookSender.sendToWebhook(binding.getWebhookUrl(), "工单助手", reply);
            return;
        }
        if (responseUrl != null && !responseUrl.trim().isEmpty()) {
            sendAibotReplyViaResponseUrl(responseUrl, reply);
            return;
        }
        if (fromWecomUserId != null && !fromWecomUserId.trim().isEmpty()) {
            sendAppMessageToWecomUser(fromWecomUserId, reply);
        }
    }

    private void sendAibotReplyViaResponseUrl(String responseUrl, String reply) {
        try {
            wecomClient.sendAibotReply(responseUrl, reply);
        } catch (Exception e) {
            log.warn("AI bot response_url回复发送失败，错误: {}", e.getMessage());
        }
    }

    private void sendAppMessageToWecomUser(String wecomUserId, String reply) {
        if (wecomUserId == null || wecomUserId.trim().isEmpty()) {
            return;
        }
        SysUserPO user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUserPO>()
                        .eq(SysUserPO::getWecomUserid, wecomUserId.trim())
                        .last("LIMIT 1")
        );
        if (user == null) {
            log.warn("单聊回复失败：未找到系统用户，wecomUserId={}", wecomUserId);
            return;
        }
        try {
            wecomClient.sendTextMessage(user.getWecomUserid(), reply);
        } catch (Exception e) {
            log.warn("单聊回复发送失败: wecomUserId={}, error={}", wecomUserId, e.getMessage());
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

    /**
     * 去除消息中的@机器人提及前缀，兼容多种格式：
     * - "@工单助手 内容"（普通@）
     * - "<@botid_xxx> 内容"（AI Bot群聊@格式）
     * - 多个连续的@提及前缀
     */
    private String stripBotMention(String content) {
        if (content == null) {
            return "";
        }
        String result = content.trim();
        // 循环去除，兼容多个@提及叠加的场景
        boolean stripped;
        do {
            stripped = false;
            String before = result;
            result = result.replaceFirst("^@工单助手[\\s\u00a0]*", "");
            result = result.replaceFirst("^<@[^>]+>[\\s\u00a0]*", "");
            if (!result.equals(before)) {
                stripped = true;
                result = result.trim();
            }
        } while (stripped && !result.isEmpty());
        return result;
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
