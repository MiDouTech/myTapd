package com.miduo.cloud.ticket.infrastructure.external.wework;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SysWeworkConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SysWeworkConfigPO;
import org.springframework.stereotype.Component;

/**
 * 企业微信运行时配置提供器
 * 优先读取数据库配置，缺失时回退到application配置。
 */
@Component
public class WeworkRuntimeConfigProvider {

    private final WecomProperties wecomProperties;
    private final SysWeworkConfigMapper sysWeworkConfigMapper;
    private final WeworkSecretCodec weworkSecretCodec;

    public WeworkRuntimeConfigProvider(WecomProperties wecomProperties,
                                       SysWeworkConfigMapper sysWeworkConfigMapper,
                                       WeworkSecretCodec weworkSecretCodec) {
        this.wecomProperties = wecomProperties;
        this.sysWeworkConfigMapper = sysWeworkConfigMapper;
        this.weworkSecretCodec = weworkSecretCodec;
    }

    public RuntimeConfig getRuntimeConfig() {
        SysWeworkConfigPO dbConfig = sysWeworkConfigMapper.selectOne(
                new LambdaQueryWrapper<SysWeworkConfigPO>()
                        .eq(SysWeworkConfigPO::getStatus, 1)
                        .orderByDesc(SysWeworkConfigPO::getUpdateTime)
                        .orderByDesc(SysWeworkConfigPO::getId)
                        .last("limit 1")
        );

        RuntimeConfig config = new RuntimeConfig();
        if (dbConfig != null) {
            config.setCorpId(dbConfig.getCorpId());
            config.setAgentId(dbConfig.getAgentId());
            config.setSecret(weworkSecretCodec.decode(dbConfig.getCorpSecret()));
            config.setContactSecret(weworkSecretCodec.decode(dbConfig.getCorpSecret()));
            config.setApiBaseUrl(defaultIfBlank(dbConfig.getApiBaseUrl(), "https://qyapi.weixin.qq.com"));
            config.setConnectTimeoutMs(defaultIfNull(dbConfig.getConnectTimeoutMs(), 10000));
            config.setReadTimeoutMs(defaultIfNull(dbConfig.getReadTimeoutMs(), 30000));
            return config;
        }

        config.setCorpId(wecomProperties.getCorpId());
        config.setAgentId(wecomProperties.getAgentId());
        config.setSecret(wecomProperties.getSecret());
        config.setContactSecret(defaultIfBlank(wecomProperties.getContactSecret(), wecomProperties.getSecret()));
        config.setApiBaseUrl("https://qyapi.weixin.qq.com");
        config.setConnectTimeoutMs(10000);
        config.setReadTimeoutMs(30000);
        return config;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    @lombok.Data
    public static class RuntimeConfig {
        private String corpId;
        private String agentId;
        private String secret;
        private String contactSecret;
        private String apiBaseUrl;
        private Integer connectTimeoutMs;
        private Integer readTimeoutMs;
    }
}
