package com.miduo.cloud.ticket.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 工单系统主启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.miduo.cloud.ticket")
@EnableAsync
@EnableScheduling
public class TicketApplication {

    public static void main(String[] args) {
        // 修复 JDK 8 与 OkHttp 3.14.4（七牛 SDK 内部依赖）的 TLS 握手兼容性问题：
        // JDK 8u341+ 默认开启 TLS 1.3，但 OkHttp 3.14.4 的 MODERN_TLS 与 JDK 8 TLS 1.3 实现存在兼容缺陷，
        // 导致上传至 up.qiniup.com 时抛出 SSLHandshakeException: handshake_failure。
        // 强制客户端 TLS 协议为 TLSv1.2，服务器侧完全支持此版本。
        if (System.getProperty("jdk.tls.client.protocols") == null) {
            System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        }
        SpringApplication.run(TicketApplication.class, args);
    }
}
