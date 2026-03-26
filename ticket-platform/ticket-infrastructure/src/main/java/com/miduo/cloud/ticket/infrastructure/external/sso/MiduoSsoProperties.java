package com.miduo.cloud.ticket.infrastructure.external.sso;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 米多星球 SSO 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "miduo.sso")
public class MiduoSsoProperties {

    /**
     * 米多星球服务基础地址
     */
    private String baseUrl;

    /**
     * 应用编码
     */
    private String appCode;

    /**
     * 应用密钥（仅后端使用，禁止暴露到前端）
     */
    private String appSecret;

    /**
     * 快捷入口 ID
     */
    private String shortcutId;

    /**
     * 登录桥回跳地址（必须 HTTPS 且在米多白名单中）
     */
    private String redirectUri;

    /**
     * 是否启用 SSO 登录
     */
    private boolean enabled = false;
}
