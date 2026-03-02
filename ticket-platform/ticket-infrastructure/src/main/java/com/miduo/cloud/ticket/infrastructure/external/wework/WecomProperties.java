package com.miduo.cloud.ticket.infrastructure.external.wework;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业微信配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "wecom")
public class WecomProperties {

    /**
     * 企业ID（CorpID）
     */
    private String corpId;

    /**
     * 自建应用AgentID
     */
    private String agentId;

    /**
     * 应用Secret
     */
    private String secret;

    /**
     * 通讯录同步Secret
     */
    private String contactSecret;

    /**
     * 回调Token
     */
    private String callbackToken;

    /**
     * 回调EncodingAESKey
     */
    private String callbackAesKey;

    /**
     * 可信域名
     */
    private String trustedDomain;
}
