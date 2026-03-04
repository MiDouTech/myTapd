package com.miduo.cloud.ticket.infrastructure.external.wework;

import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 企微Access Token管理器
 * 使用Redis缓存access_token，有效期2小时，提前10分钟刷新
 */
@Component
public class WecomTokenManager {

    private static final Logger log = LoggerFactory.getLogger(WecomTokenManager.class);

    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final long TOKEN_EXPIRE_BUFFER_SECONDS = 600;

    private final WeworkRuntimeConfigProvider runtimeConfigProvider;
    private final StringRedisTemplate stringRedisTemplate;

    public WecomTokenManager(WeworkRuntimeConfigProvider runtimeConfigProvider, StringRedisTemplate stringRedisTemplate) {
        this.runtimeConfigProvider = runtimeConfigProvider;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 获取应用access_token（带缓存）
     */
    public String getAccessToken() {
        WeworkRuntimeConfigProvider.RuntimeConfig config = runtimeConfigProvider.getRuntimeConfig();
        return getToken(RedisKeyConstants.WECOM_ACCESS_TOKEN, config.getCorpId(), config.getSecret());
    }

    /**
     * 获取通讯录access_token（带缓存）
     */
    public String getContactAccessToken() {
        WeworkRuntimeConfigProvider.RuntimeConfig config = runtimeConfigProvider.getRuntimeConfig();
        String contactSecret = config.getContactSecret();
        if (contactSecret == null || contactSecret.isEmpty()) {
            return getAccessToken();
        }
        return getToken(RedisKeyConstants.WECOM_CONTACT_ACCESS_TOKEN, config.getCorpId(), contactSecret);
    }

    private String getToken(String cacheKey, String corpId, String secret) {
        if (corpId == null || corpId.trim().isEmpty() || secret == null || secret.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "企业微信配置不完整，请先配置corpId和corpSecret");
        }
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        String url = GET_TOKEN_URL + "?corpid=" + corpId + "&corpsecret=" + secret;
        String response = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(response);

        if (json == null || json.getIntValue("errcode") != 0) {
            String errMsg = json != null ? json.getString("errmsg") : "response is null";
            log.error("获取企微access_token失败: {}", errMsg);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "获取企微access_token失败: " + errMsg);
        }

        String accessToken = json.getString("access_token");
        long expiresIn = json.getLongValue("expires_in");
        long cacheSeconds = Math.max(expiresIn - TOKEN_EXPIRE_BUFFER_SECONDS, 60);

        stringRedisTemplate.opsForValue().set(cacheKey, accessToken, cacheSeconds, TimeUnit.SECONDS);
        log.info("获取企微access_token成功，缓存{}秒", cacheSeconds);

        return accessToken;
    }

    /**
     * 强制刷新access_token
     */
    public void refreshAccessToken() {
        stringRedisTemplate.delete(RedisKeyConstants.WECOM_ACCESS_TOKEN);
        stringRedisTemplate.delete(RedisKeyConstants.WECOM_CONTACT_ACCESS_TOKEN);
        getAccessToken();
    }
}
