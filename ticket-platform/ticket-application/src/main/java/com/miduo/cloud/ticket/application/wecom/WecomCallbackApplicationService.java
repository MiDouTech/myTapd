package com.miduo.cloud.ticket.application.wecom;

import cn.hutool.crypto.SecureUtil;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.wecom.mq.WecomMessagePublisher;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.WecomBotMessageStatus;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomCallbackMessageDTO;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomCallbackCryptoService;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomXmlParser;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomBotMessageLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomBotMessageLogPO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 企微回调应用服务
 */
@Service
public class WecomCallbackApplicationService extends BaseApplicationService {

    private static final long DEDUP_TTL_HOURS = 24L;

    private final WecomCallbackCryptoService callbackCryptoService;
    private final WecomMessagePublisher messagePublisher;
    private final StringRedisTemplate redisTemplate;
    private final WecomBotMessageLogMapper botMessageLogMapper;

    public WecomCallbackApplicationService(WecomCallbackCryptoService callbackCryptoService,
                                           WecomMessagePublisher messagePublisher,
                                           StringRedisTemplate redisTemplate,
                                           WecomBotMessageLogMapper botMessageLogMapper) {
        this.callbackCryptoService = callbackCryptoService;
        this.messagePublisher = messagePublisher;
        this.redisTemplate = redisTemplate;
        this.botMessageLogMapper = botMessageLogMapper;
    }

    /**
     * URL验证
     */
    public String verifyUrl(String msgSignature, String timestamp, String nonce, String echostr) {
        return callbackCryptoService.verifyAndDecodeEcho(msgSignature, timestamp, nonce, echostr);
    }

    /**
     * 接收并分发回调消息（异步处理）
     */
    public void receiveCallback(String msgSignature, String timestamp, String nonce, String requestBody) {
        if (requestBody == null || requestBody.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.WECOM_MSG_PARSE_FAILED, "回调消息体为空");
        }

        String encrypted = WecomXmlParser.extractEncryptField(requestBody);

        String plainXml = callbackCryptoService.decryptMessage(msgSignature, timestamp, nonce, encrypted);
        Map<String, String> messageMap = WecomXmlParser.parseDecryptedMessage(plainXml);
        WecomCallbackMessageDTO message = buildMessage(messageMap, plainXml);
        log.info("收到企微回调消息: msgId={}, msgType={}, chatId={}, fromWecomUserid={}",
                message.getMsgId(), message.getMsgType(), message.getChatId(), message.getFromWecomUserid());

        if (isDuplicate(message.getMsgId())) {
            saveDuplicateLog(message);
            log.info("企微回调消息去重命中: msgId={}, chatId={}", message.getMsgId(), message.getChatId());
            return;
        }

        if (!"text".equalsIgnoreCase(message.getMsgType())) {
            saveIgnoredLog(message, "非文本消息已忽略");
            log.info("企微回调消息已忽略: msgId={}, msgType={}, reason=非文本消息", message.getMsgId(), message.getMsgType());
            return;
        }

        messagePublisher.publish(message);
        log.info("企微回调消息已投递异步处理: msgId={}, chatId={}", message.getMsgId(), message.getChatId());
    }

    private WecomCallbackMessageDTO buildMessage(Map<String, String> messageMap, String plainXml) {
        WecomCallbackMessageDTO message = new WecomCallbackMessageDTO();
        message.setMsgType(safeValue(messageMap.get("MsgType")));
        message.setChatId(resolveChatId(messageMap));
        message.setFromWecomUserid(safeValue(messageMap.get("FromUserName")));
        message.setContent(safeValue(messageMap.get("Content")));
        message.setRawXml(plainXml);
        message.setCreateTime(safeValue(messageMap.get("CreateTime")));
        message.setResponseUrl(safeValue(messageMap.get("ResponseUrl")));

        String msgId = safeValue(messageMap.get("MsgId"));
        if (msgId.isEmpty()) {
            msgId = SecureUtil.sha1(message.getFromWecomUserid() + "|" + message.getCreateTime() + "|" + message.getContent());
        }
        message.setMsgId(msgId);
        return message;
    }

    private String resolveChatId(Map<String, String> messageMap) {
        String chatId = safeValue(messageMap.get("ChatId"));
        if (!chatId.isEmpty()) {
            return chatId;
        }
        return safeValue(messageMap.get("ToUserName"));
    }

    private boolean isDuplicate(String msgId) {
        if (msgId == null || msgId.trim().isEmpty()) {
            return false;
        }
        String key = RedisKeyConstants.WECOM_CALLBACK_MSG_DEDUP_PREFIX + msgId.trim();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
        return !Boolean.TRUE.equals(first);
    }

    private void saveDuplicateLog(WecomCallbackMessageDTO message) {
        WecomBotMessageLogPO logPO = new WecomBotMessageLogPO();
        logPO.setChatId(message.getChatId());
        logPO.setMsgId(message.getMsgId());
        logPO.setFromWecomUserid(message.getFromWecomUserid());
        logPO.setRawMessage(message.getRawXml());
        logPO.setStatus(WecomBotMessageStatus.DUPLICATE.getCode());
        logPO.setErrorMsg("重复消息");
        botMessageLogMapper.insert(logPO);
    }

    private void saveIgnoredLog(WecomCallbackMessageDTO message, String reason) {
        WecomBotMessageLogPO logPO = new WecomBotMessageLogPO();
        logPO.setChatId(message.getChatId());
        logPO.setMsgId(message.getMsgId());
        logPO.setFromWecomUserid(message.getFromWecomUserid());
        logPO.setRawMessage(message.getRawXml());
        logPO.setStatus(WecomBotMessageStatus.IGNORED.getCode());
        logPO.setErrorMsg(reason);
        botMessageLogMapper.insert(logPO);
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }
}
