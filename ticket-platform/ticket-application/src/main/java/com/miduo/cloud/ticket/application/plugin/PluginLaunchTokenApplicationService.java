package com.miduo.cloud.ticket.application.plugin;

import com.miduo.cloud.ticket.application.integration.IntegrationAppCredentialResolver;
import com.miduo.cloud.ticket.application.integration.ResolvedIntegrationClient;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.PluginPermission;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginLaunchTokenInput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginLaunchTokenOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po.IntegrationAppPO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * LaunchToken 签发
 */
@Service
public class PluginLaunchTokenApplicationService {

    private final IntegrationAppCredentialResolver credentialResolver;
    private final PluginUserMappingService pluginUserMappingService;
    private final PluginLaunchTokenPort pluginLaunchTokenPort;
    private final long launchTokenExpireSeconds;

    public PluginLaunchTokenApplicationService(IntegrationAppCredentialResolver credentialResolver,
                                               PluginUserMappingService pluginUserMappingService,
                                               PluginLaunchTokenPort pluginLaunchTokenPort,
                                               @Value("${plugin.launch-token-expire-seconds:300}") long launchTokenExpireSeconds) {
        this.credentialResolver = credentialResolver;
        this.pluginUserMappingService = pluginUserMappingService;
        this.pluginLaunchTokenPort = pluginLaunchTokenPort;
        this.launchTokenExpireSeconds = launchTokenExpireSeconds;
    }

    public PluginLaunchTokenOutput issueLaunchToken(String appKey, PluginLaunchTokenInput input) {
        ResolvedIntegrationClient client = credentialResolver.resolve(appKey);
        if (client == null) {
            throw BusinessException.of(ErrorCode.OPEN_API_APP_NOT_FOUND, "接入应用不存在或已禁用");
        }
        if (!credentialResolver.hasPermission(client, PluginPermission.LAUNCH_TOKEN)) {
            throw BusinessException.of(ErrorCode.OPEN_API_PERMISSION_DENIED, "当前应用无 LaunchToken 签发权限");
        }
        IntegrationAppPO app = credentialResolver.requireEnabledApp(client.getIntegrationAppId());
        if (app == null) {
            throw BusinessException.of(ErrorCode.PLUGIN_APP_DISABLED, "接入应用已禁用");
        }
        Long userId = pluginUserMappingService.resolveOrCreateUser(
                client.getSystemCode(),
                input.getExternalUserId(),
                input.getUserName(),
                input.getDept(),
                input.getMobile());
        String token = pluginLaunchTokenPort.issueToken(
                client.getIntegrationAppId(),
                client.getAppKey(),
                input.getExternalUserId().trim(),
                userId);
        PluginLaunchTokenOutput output = new PluginLaunchTokenOutput();
        output.setLaunchToken(token);
        output.setExpireSeconds(launchTokenExpireSeconds);
        return output;
    }

    public PluginLaunchTokenClaims requireValidToken(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        PluginLaunchTokenClaims claims = pluginLaunchTokenPort.validateToken(token);
        if (claims == null || claims.getIntegrationAppId() == null || claims.getUserId() == null) {
            throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "LaunchToken 无效");
        }
        IntegrationAppPO app = credentialResolver.requireEnabledApp(claims.getIntegrationAppId());
        if (app == null) {
            throw BusinessException.of(ErrorCode.PLUGIN_APP_DISABLED, "接入应用已禁用");
        }
        return claims;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "缺少 Authorization 请求头");
        }
        String trimmed = authorizationHeader.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = trimmed.substring(7).trim();
            if (!StringUtils.hasText(token)) {
                throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "LaunchToken 不能为空");
            }
            return token;
        }
        throw BusinessException.of(ErrorCode.PLUGIN_LAUNCH_TOKEN_INVALID, "Authorization 格式错误");
    }
}
