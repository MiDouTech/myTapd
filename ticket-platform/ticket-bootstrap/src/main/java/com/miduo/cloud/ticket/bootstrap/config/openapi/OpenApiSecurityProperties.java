package com.miduo.cloud.ticket.bootstrap.config.openapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 开放接口安全配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "open-api")
public class OpenApiSecurityProperties {

    /**
     * 开放接口开关
     */
    private boolean enabled = true;

    /**
     * 时间戳允许误差（秒）
     */
    private long timestampSkewSeconds = 300L;

    /**
     * nonce 去重有效期（秒）
     */
    private long nonceExpireSeconds = 300L;

    /**
     * 单 AppKey 每分钟限流
     */
    private long rateLimitPerMinute = 120L;

    /**
     * 客户端列表
     */
    private List<Client> clients = new ArrayList<>();

    public Client findEnabledClient(String appKey) {
        if (!StringUtils.hasText(appKey) || clients == null || clients.isEmpty()) {
            return null;
        }
        for (Client client : clients) {
            if (client == null || !Boolean.TRUE.equals(client.getEnabled())) {
                continue;
            }
            if (appKey.trim().equals(client.getAppKey())) {
                return client;
            }
        }
        return null;
    }

    public List<Client> getClients() {
        if (clients == null) {
            return Collections.emptyList();
        }
        return clients;
    }

    @Data
    public static class Client {
        private String appKey;
        private String appSecret;
        private String appName;
        private Boolean enabled = true;
    }
}
