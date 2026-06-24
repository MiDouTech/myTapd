package com.miduo.cloud.ticket.bootstrap.config.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.ticket.application.integration.IntegrationAppCredentialResolver;
import com.miduo.cloud.ticket.application.integration.ResolvedIntegrationClient;
import com.miduo.cloud.ticket.common.constants.OpenApiAuthConstants;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.PluginPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * 开放接口 AppKey + AppSecret 鉴权过滤器
 */
@Component
public class OpenApiAppAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OpenApiAppAuthFilter.class);
    private static final String PATH_PREFIX = "/api/open/v1/";
    private static final String PLUGIN_LAUNCH_TOKEN_PATH = "/api/open/v1/plugin/launch-token";
    private static final String PLUGIN_CONFIG_PATH = "/api/open/v1/plugin/config";

    private final OpenApiSecurityProperties securityProperties;
    private final IntegrationAppCredentialResolver integrationAppCredentialResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public OpenApiAppAuthFilter(OpenApiSecurityProperties securityProperties,
                                IntegrationAppCredentialResolver integrationAppCredentialResolver,
                                StringRedisTemplate stringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.integrationAppCredentialResolver = integrationAppCredentialResolver;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith(PATH_PREFIX)) {
            return true;
        }
        if (uri.startsWith("/api/open/v1/plugin/")) {
            if (PLUGIN_CONFIG_PATH.equals(uri) && "GET".equalsIgnoreCase(request.getMethod())) {
                return true;
            }
            return !PLUGIN_LAUNCH_TOKEN_PATH.equals(uri);
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!securityProperties.isEnabled()) {
            writeFail(response, ErrorCode.OPEN_API_APP_NOT_FOUND, "开放接口未启用");
            return;
        }

        String appKey = trimToEmpty(request.getHeader(OpenApiAuthConstants.HEADER_APP_KEY));
        String timestamp = trimToEmpty(request.getHeader(OpenApiAuthConstants.HEADER_TIMESTAMP));
        String nonce = trimToEmpty(request.getHeader(OpenApiAuthConstants.HEADER_NONCE));
        String signature = trimToEmpty(request.getHeader(OpenApiAuthConstants.HEADER_SIGNATURE));
        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature)) {
            writeFail(response, ErrorCode.OPEN_API_SIGNATURE_INVALID, "缺少开放接口鉴权请求头");
            return;
        }

        long timestampSec;
        try {
            long parsed = Long.parseLong(timestamp);
            timestampSec = parsed > 9999999999L ? parsed / 1000L : parsed;
        } catch (NumberFormatException ex) {
            writeFail(response, ErrorCode.OPEN_API_TIMESTAMP_EXPIRED, "时间戳格式错误");
            return;
        }
        long nowSec = Instant.now().getEpochSecond();
        if (Math.abs(nowSec - timestampSec) > securityProperties.getTimestampSkewSeconds()) {
            writeFail(response, ErrorCode.OPEN_API_TIMESTAMP_EXPIRED, "请求时间已过期");
            return;
        }

        String appSecret = resolveAppSecret(appKey);
        if (!StringUtils.hasText(appSecret)) {
            writeFail(response, ErrorCode.OPEN_API_APP_NOT_FOUND, "AppKey 不存在或已禁用");
            return;
        }

        if (PLUGIN_LAUNCH_TOKEN_PATH.equals(request.getRequestURI())) {
            ResolvedIntegrationClient dbClient = integrationAppCredentialResolver.resolve(appKey);
            if (dbClient == null || !integrationAppCredentialResolver.hasPermission(dbClient, PluginPermission.LAUNCH_TOKEN)) {
                writeFail(response, ErrorCode.OPEN_API_PERMISSION_DENIED, "当前应用无 LaunchToken 签发权限");
                return;
            }
        }

        String nonceKey = RedisKeyConstants.OPEN_API_NONCE_PREFIX + appKey + ":" + nonce;
        Boolean firstSeen = stringRedisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", securityProperties.getNonceExpireSeconds(), TimeUnit.SECONDS);
        if (firstSeen != null && !firstSeen) {
            writeFail(response, ErrorCode.OPEN_API_REPLAY_REQUEST, "重复请求，请更换 nonce");
            return;
        }

        String minuteBucket = String.valueOf(nowSec / 60L);
        String limitKey = RedisKeyConstants.OPEN_API_RATE_LIMIT_PREFIX + appKey + ":" + minuteBucket;
        Long current = stringRedisTemplate.opsForValue().increment(limitKey);
        if (current != null && current == 1L) {
            stringRedisTemplate.expire(limitKey, 120, TimeUnit.SECONDS);
        }
        if (current != null && current > securityProperties.getRateLimitPerMinute()) {
            writeFail(response, ErrorCode.OPEN_API_RATE_LIMITED, "调用频率超过限制，请稍后重试");
            return;
        }

        String expectedSignature = buildSignature(request, appKey, timestamp, nonce, appSecret);
        if (!signature.equalsIgnoreCase(expectedSignature)) {
            log.warn("开放接口签名不通过: appKey={}, uri={}", appKey, request.getRequestURI());
            writeFail(response, ErrorCode.OPEN_API_SIGNATURE_INVALID, "签名校验失败");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String resolveAppSecret(String appKey) {
        ResolvedIntegrationClient dbClient = integrationAppCredentialResolver.resolve(appKey);
        if (dbClient != null && StringUtils.hasText(dbClient.getAppSecret())) {
            return dbClient.getAppSecret();
        }
        OpenApiSecurityProperties.Client yamlClient = securityProperties.findEnabledClient(appKey);
        if (yamlClient == null || !StringUtils.hasText(yamlClient.getAppSecret())) {
            return null;
        }
        return yamlClient.getAppSecret();
    }

    private String buildSignature(HttpServletRequest request,
                                  String appKey,
                                  String timestamp,
                                  String nonce,
                                  String appSecret) {
        String canonical = request.getMethod().toUpperCase() + OpenApiAuthConstants.SIGN_JOINER
                + request.getRequestURI() + OpenApiAuthConstants.SIGN_JOINER
                + buildCanonicalQuery(request.getParameterMap()) + OpenApiAuthConstants.SIGN_JOINER
                + timestamp + OpenApiAuthConstants.SIGN_JOINER
                + nonce + OpenApiAuthConstants.SIGN_JOINER
                + appKey;
        return hmacSha256Hex(canonical, appSecret);
    }

    private String buildCanonicalQuery(Map<String, String[]> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "";
        }
        TreeMap<String, List<String>> sorted = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (!StringUtils.hasText(entry.getKey())) {
                continue;
            }
            List<String> values = new ArrayList<>();
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    values.add(value == null ? "" : value);
                }
            }
            Collections.sort(values);
            sorted.put(entry.getKey(), values);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : sorted.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(entry.getKey()).append('=');
            builder.append(String.join(",", entry.getValue()));
        }
        return builder.toString();
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(OpenApiAuthConstants.HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                    OpenApiAuthConstants.HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String item = Integer.toHexString(b & 0xff);
                if (item.length() < 2) {
                    hex.append('0');
                }
                hex.append(item);
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("开放接口签名生成失败", ex);
        }
    }

    private void writeFail(HttpServletResponse response, ErrorCode errorCode, String detail) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResult.fail(errorCode.getCode(), detail));
    }

    private String trimToEmpty(String text) {
        return text == null ? "" : text.trim();
    }
}
