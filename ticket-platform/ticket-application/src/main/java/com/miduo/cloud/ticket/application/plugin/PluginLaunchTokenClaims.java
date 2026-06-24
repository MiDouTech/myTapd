package com.miduo.cloud.ticket.application.plugin;

import lombok.Data;

import java.io.Serializable;

/**
 * LaunchToken 解析结果
 */
@Data
public class PluginLaunchTokenClaims implements Serializable {

    private Long integrationAppId;

    private String appKey;

    private String externalUserId;

    private Long userId;

    private String jti;
}
