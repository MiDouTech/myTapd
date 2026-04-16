package com.miduo.cloud.ticket.application.wecom;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.notification.sender.WecomGroupWebhookSender;
import com.miduo.cloud.ticket.application.ticket.TicketBugApplicationService;
import com.miduo.cloud.ticket.application.wecom.model.WecomBotParseResult;
import com.miduo.cloud.ticket.application.wecom.model.WecomDraftSession;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.enums.WecomBotCommandType;
import com.miduo.cloud.ticket.common.enums.WecomBotMessageStatus;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketBugCustomerInfoInput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpAnalyzeResult;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomCallbackMessageDTO;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomMessageParseOutput;
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
    private final WecomMessageFieldParser wecomMessageFieldParser;
    private final TicketBugApplicationService ticketBugApplicationService;

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
                                 WecomImageHandlerService imageHandlerService,
                                 WecomMessageFieldParser wecomMessageFieldParser,
                                 TicketBugApplicationService ticketBugApplicationService) {
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
        this.wecomMessageFieldParser = wecomMessageFieldParser;
        this.ticketBugApplicationService = ticketBugApplicationService;
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

        if (!containsProblemDescLabel(rawText)) {
            String promptMsg = buildMissingProblemDescPrompt();
            sendReply(binding, promptMsg, fromWecomUserId, message.getResponseUrl());
            saveLog(message, parseResult, null, WecomBotMessageStatus.FAIL, "消息缺少问题描述字段",
                    PARSE_TYPE_NATURAL_LANGUAGE, null);
            log.info("企微消息缺少问题描述字段，已提示用户补充: msgId={}, chatId={}", message.getMsgId(), chatId);
            return;
        }

        NlpAnalyzeResult nlpResult = naturalLangParser.analyze(rawText, defaultCategoryPath);

        String title = extractProblemDescAsTitle(rawText);
        if (title == null || title.trim().isEmpty()) {
            title = nlpResult.getTitle();
        }

        WecomDraftSession draft = new WecomDraftSession();
        draft.setSessionKey(chatId != null ? chatId : fromWecomUserId);
        draft.setWecomUserId(fromWecomUserId);
        draft.setTitle(title);
        draft.setCategoryPath(nlpResult.getCategoryPath());
        draft.setPriority(nlpResult.getPriority());
        draft.setDescription(formatNaturalLanguageDescriptionForStorage(rawText));
        draft.setNlpConfidence(nlpResult.getConfidence());
        draft.setChatId(chatId);

        TicketPO existingTicket = findExistingTicketByDescription(rawText);
        if (existingTicket != null) {
            String duplicateReply = buildDuplicateTicketReply(existingTicket);
            sendReply(binding, duplicateReply, fromWecomUserId, message.getResponseUrl());
            saveLog(message, parseResult, existingTicket.getId(), WecomBotMessageStatus.SUCCESS,
                    "描述内容重复，已存在工单: " + existingTicket.getTicketNo(),
                    PARSE_TYPE_NATURAL_LANGUAGE, nlpResult.getConfidence() != null ? nlpResult.getConfidence().byteValue() : null);
            log.info("企微消息描述内容与已有工单重复，跳过创建: msgId={}, chatId={}, existingTicketNo={}",
                    message.getMsgId(), chatId, existingTicket.getTicketNo());
            return;
        }

        Long ticketId = interactiveConfirmService.createTicketDirectly(draft, chatId, fromWecomUserId);
        if (ticketId != null) {
            autoFillCustomerInfoFromMessage(ticketId, rawText);
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

    /**
     * 企微自然语言建单常把多段「字段名：值」拼成一行，入库前插入换行便于详情页阅读。
     */
    private String formatNaturalLanguageDescriptionForStorage(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return rawText;
        }
        String text = rawText.trim().replace("\r\n", "\n");
        text = text.replaceFirst("^机器人\\s+(?=(?:商户编号|公司名称|商户账号|场景码|问题描述|预期结果)\\s*[:：])",
                "机器人\n");
        return text.replaceAll("(?<!\\n)\\s+((?:商户编号|公司名称|商户账号|场景码|问题描述|预期结果)\\s*[:：])",
                "\n$1");
    }

    /**
     * 检查消息是否包含"问题描述"标签（支持中英文冒号）
     */
    private boolean containsProblemDescLabel(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return text.contains("问题描述：") || text.contains("问题描述:");
    }

    /**
     * 从消息中提取"问题描述"字段值作为工单标题，最大截取300字符
     */
    private String extractProblemDescAsTitle(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }
        WecomMessageParseOutput parseOutput = wecomMessageFieldParser.parse(rawText);
        if (parseOutput == null || parseOutput.getProblemDesc() == null || parseOutput.getProblemDesc().trim().isEmpty()) {
            return null;
        }
        String problemDesc = parseOutput.getProblemDesc().trim();
        if (problemDesc.length() > 300) {
            return problemDesc.substring(0, 300);
        }
        return problemDesc;
    }

    /**
     * 构建缺少"问题描述"字段时的提示消息
     */
    private String buildMissingProblemDescPrompt() {
        return "如果需要创建工单，请补充问题描述内容。格式如下：\n" +
                "商户编号：XXX\n" +
                "公司名称：XXX\n" +
                "商户账号：XXX\n" +
                "问题描述：XXX（必填）\n" +
                "预期结果：XXX\n" +
                "场景码：XXX";
    }

    /**
     * 从企微消息原文中自动提取客服信息字段，并持久化到缺陷工单的客服信息记录中
     * 仅在工单刚创建、客服信息记录不存在时执行（幂等保护）
     */
    private void autoFillCustomerInfoFromMessage(Long ticketId, String rawText) {
        if (ticketId == null || rawText == null || rawText.trim().isEmpty()) {
            return;
        }
        try {
            WecomMessageParseOutput parseOutput = wecomMessageFieldParser.parse(rawText);
            if (parseOutput == null || parseOutput.getMatchedFields() == null || parseOutput.getMatchedFields().isEmpty()) {
                log.info("企微消息未提取到客服信息字段，跳过自动填充: ticketId={}", ticketId);
                return;
            }
            TicketBugCustomerInfoInput input = new TicketBugCustomerInfoInput();
            input.setMerchantNo(parseOutput.getMerchantNo() != null ? parseOutput.getMerchantNo() : "");
            input.setCompanyName(parseOutput.getCompanyName() != null ? parseOutput.getCompanyName() : "");
            input.setMerchantAccount(parseOutput.getMerchantAccount() != null ? parseOutput.getMerchantAccount() : "");
            input.setSceneCode(parseOutput.getSceneCode() != null ? parseOutput.getSceneCode() : "");
            input.setProblemDesc(parseOutput.getProblemDesc() != null ? parseOutput.getProblemDesc() : "");
            input.setExpectedResult(parseOutput.getExpectedResult() != null ? parseOutput.getExpectedResult() : "");
            input.setProblemScreenshot(parseOutput.getProblemScreenshot() != null ? parseOutput.getProblemScreenshot() : "");
            ticketBugApplicationService.initCustomerInfoFromBot(ticketId, input);
            log.info("企微消息客服信息自动填充完成: ticketId={}, matchedFields={}, confidence={}",
                    ticketId, parseOutput.getMatchedFields(), parseOutput.getConfidence());
        } catch (Exception e) {
            log.warn("企微消息客服信息自动填充失败，已降级跳过: ticketId={}, error={}", ticketId, e.getMessage());
        }
    }

    /**
     * 根据描述内容查找已存在的未关闭工单（精确匹配 description 全文）
     * 仅匹配非终态工单，终态工单允许重复提交
     */
    private TicketPO findExistingTicketByDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        List<String> terminalStatuses = Arrays.asList(
                TicketStatus.COMPLETED.getCode(),
                TicketStatus.CLOSED.getCode(),
                TicketStatus.REJECTED.getCode()
        );
        LambdaQueryWrapper<TicketPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketPO::getDescription, description.trim())
                .notIn(TicketPO::getStatus, terminalStatuses)
                .orderByDesc(TicketPO::getCreateTime)
                .last("LIMIT 1");
        return ticketMapper.selectOne(wrapper);
    }

    private String buildDuplicateTicketReply(TicketPO ticket) {
        String ticketNo = ticket.getTicketNo();
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        String statusLabel = status != null ? status.getLabel() : ticket.getStatus();
        String publicLink = buildPublicTicketLink(ticketNo);
        return "【此问题已创建过工单，如需再创建，请修改描述内容】\n" +
                "工单编号：" + safeValue(ticketNo) + "\n" +
                "工单状态：" + statusLabel + "\n" +
                "查看详情：" + publicLink;
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
        return WecomPublicLinkBuilder.buildPublicTicketLink(wecomProperties.getTrustedDomain(), ticketNo);
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
     * - "@工单助手机器人 内容"（企业微信群聊中显示名称带后缀时）
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
            // \S* 匹配@工单助手后面可能跟随的任意非空白后缀（如"机器人"）
            result = result.replaceFirst("^@工单助手\\S*[\\s\u00a0]*", "");
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
