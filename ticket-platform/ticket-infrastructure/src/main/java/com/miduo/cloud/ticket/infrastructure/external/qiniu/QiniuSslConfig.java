package com.miduo.cloud.ticket.infrastructure.external.qiniu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 七牛云 TLS 兼容配置
 *
 * 问题根因：
 * 七牛云 Java SDK 7.14.0 内部使用 OkHttp 3.14.4。OkHttp 的 MODERN_TLS ConnectionSpec 包含
 * TLS 1.3 协议（TLS_AES_256_GCM_SHA384 等）。当 JDK 8u341+ 默认开启 TLS 1.3 后，
 * 与 up.qiniup.com（TrustAsia CA 2025）握手时，JDK 8 的 TLS 1.3 实现与 OkHttp 3.14.4
 * 的 cipher suite 配置存在兼容问题，导致：
 * javax.net.ssl.SSLHandshakeException: Received fatal alert: handshake_failure
 *
 * 修复策略：
 * 通过 JVM 系统属性 jdk.tls.client.protocols 限制客户端 TLS 协商版本为 TLSv1.2，
 * 避免 TLS 1.3 握手失败。TLSv1.2 对 JDK 8 和 OkHttp 3.14.4 完全兼容。
 * 此配置对所有通过 JDK SSL 发起的出站 HTTPS 连接生效，包括七牛 SDK 内部的 OkHttp 客户端。
 */
@Configuration
public class QiniuSslConfig {

    private static final Logger log = LoggerFactory.getLogger(QiniuSslConfig.class);

    @PostConstruct
    public void configureTlsForQiniu() {
        String currentProtocols = System.getProperty("jdk.tls.client.protocols");
        if (currentProtocols == null || currentProtocols.isEmpty()) {
            System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
            log.info("七牛云 TLS 兼容配置：已设置 jdk.tls.client.protocols=TLSv1.2，修复 JDK 8 与 OkHttp 3.14.4 的 TLS 握手兼容性问题");
        } else {
            log.info("七牛云 TLS 兼容配置：jdk.tls.client.protocols 已由外部配置为 {}，跳过覆盖", currentProtocols);
        }
    }
}
