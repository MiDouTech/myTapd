package com.miduo.cloud.ticket.bootstrap.config.plugin;

import com.miduo.cloud.ticket.application.plugin.PluginLaunchTokenClaims;
import com.miduo.cloud.ticket.application.plugin.PluginLaunchTokenPort;
import com.miduo.cloud.ticket.bootstrap.config.jwt.JwtTokenProvider;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * LaunchToken 签发与单次消费校验
 */
@Service
public class PluginLaunchTokenPortImpl implements PluginLaunchTokenPort {

    private static final String TOKEN_TYPE_LAUNCH = "launch";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final long launchTokenExpireSeconds;

    public PluginLaunchTokenPortImpl(JwtTokenProvider jwtTokenProvider,
                                     StringRedisTemplate stringRedisTemplate,
                                     @Value("${plugin.launch-token-expire-seconds:300}") long launchTokenExpireSeconds) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.stringRedisTemplate = stringRedisTemplate;
        this.launchTokenExpireSeconds = launchTokenExpireSeconds;
    }

    @Override
    public String issueToken(Long integrationAppId,
                             String appKey,
                             String externalUserId,
                             Long userId) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        String token = jwtTokenProvider.createLaunchToken(
                integrationAppId, appKey, externalUserId, userId, jti, launchTokenExpireSeconds);
        String redisKey = RedisKeyConstants.PLUGIN_LAUNCH_JTI_PREFIX + jti;
        stringRedisTemplate.opsForValue().set(redisKey, "1", launchTokenExpireSeconds, TimeUnit.SECONDS);
        return token;
    }

    @Override
    public PluginLaunchTokenClaims validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "LaunchToken 不能为空");
        }
        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(token.trim());
        } catch (ExpiredJwtException ex) {
            throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "LaunchToken 已过期");
        } catch (JwtException ex) {
            throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "LaunchToken 无效");
        }
        if (!TOKEN_TYPE_LAUNCH.equals(claims.get("type", String.class))) {
            throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "LaunchToken 类型错误");
        }
        String jti = claims.get("jti", String.class);
        if (!StringUtils.hasText(jti)) {
            throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "LaunchToken 缺少标识");
        }
        String redisKey = RedisKeyConstants.PLUGIN_LAUNCH_JTI_PREFIX + jti;
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (exists == null || !exists) {
            throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_USED, "LaunchToken 已失效");
        }
        PluginLaunchTokenClaims result = new PluginLaunchTokenClaims();
        result.setIntegrationAppId(claims.get("integrationAppId", Long.class));
        result.setAppKey(claims.get("appKey", String.class));
        result.setExternalUserId(claims.get("externalUserId", String.class));
        result.setUserId(claims.get("userId", Long.class));
        result.setJti(jti);
        return result;
    }
}
