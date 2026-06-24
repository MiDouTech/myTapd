package com.miduo.cloud.ticket.application.plugin;

/**
 * LaunchToken 签发与校验端口（由 bootstrap 实现）
 */
public interface PluginLaunchTokenPort {

    String issueToken(Long integrationAppId,
                      String appKey,
                      String externalUserId,
                      Long userId);

    PluginLaunchTokenClaims validateToken(String token);
}
