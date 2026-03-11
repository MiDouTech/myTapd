package com.miduo.cloud.ticket.application.wecom;

import com.alibaba.fastjson2.JSON;
import com.miduo.cloud.ticket.application.wecom.model.WecomDraftSession;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 企微工单草稿会话管理服务
 * 基于 Redis 存储，支持群聊（60s TTL）和私聊（300s TTL）
 * Task023：企微文本消息自动创建工单
 */
@Service
public class WecomDraftSessionService {

    /**
     * 群聊草稿超时时间（秒）
     */
    private static final long GROUP_TTL_SECONDS = 60L;

    /**
     * 私聊草稿超时时间（秒）
     */
    private static final long PRIVATE_TTL_SECONDS = 300L;

    private final StringRedisTemplate redisTemplate;

    public WecomDraftSessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存草稿会话（新会话覆盖旧会话）
     *
     * @param chatOrUserId   群聊chatId 或 私聊wecomUserId
     * @param wecomUserId    发送人企微UserId
     * @param session        草稿会话内容
     * @param isGroup        是否为群聊
     */
    public void saveDraft(String chatOrUserId, String wecomUserId,
                          WecomDraftSession session, boolean isGroup) {
        String key = buildKey(chatOrUserId, wecomUserId);
        long ttl = isGroup ? GROUP_TTL_SECONDS : PRIVATE_TTL_SECONDS;
        redisTemplate.opsForValue().set(key, JSON.toJSONString(session), ttl, TimeUnit.SECONDS);
    }

    /**
     * 获取草稿会话
     *
     * @param chatOrUserId 群聊chatId 或 私聊wecomUserId
     * @param wecomUserId  发送人企微UserId
     * @return 草稿会话，不存在则返回null
     */
    public WecomDraftSession getDraft(String chatOrUserId, String wecomUserId) {
        String key = buildKey(chatOrUserId, wecomUserId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, WecomDraftSession.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除草稿会话
     *
     * @param chatOrUserId 群聊chatId 或 私聊wecomUserId
     * @param wecomUserId  发送人企微UserId
     */
    public void removeDraft(String chatOrUserId, String wecomUserId) {
        String key = buildKey(chatOrUserId, wecomUserId);
        redisTemplate.delete(key);
    }

    private String buildKey(String chatOrUserId, String wecomUserId) {
        return RedisKeyConstants.WECOM_DRAFT_SESSION_PREFIX + chatOrUserId + ":" + wecomUserId;
    }
}
