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
        // Why: 预览/CDS 环境里 Nacos secrets 可能短时不可用，这里把已有环境变量提升为系统属性兜底，
        // 避免出现“datasource url 缺失 / jwt secret 为空”导致应用无法启动。
        applyRuntimeConfigFallback();
        SpringApplication.run(TicketApplication.class, args);
    }

    private static void applyRuntimeConfigFallback() {
        String datasourceUrl = firstNonBlank(
                System.getenv("DATASOURCE_URL"),
                System.getenv("SPRING_DATASOURCE_URL")
        );
        if (isBlank(datasourceUrl)) {
            String mysqlHost = trimToNull(System.getenv("MYSQL_HOST"));
            if (mysqlHost != null) {
                String mysqlPort = firstNonBlank(System.getenv("MYSQL_PORT"), "3306");
                String mysqlDb = firstNonBlank(System.getenv("MYSQL_DB"), "ticket_platform");
                datasourceUrl = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDb
                        + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
            }
        }
        if (isBlank(datasourceUrl) && shouldEnableDevPreviewFallback()) {
            // Why: 预览环境只要能拉起服务即可，不强依赖数据库连通性
            datasourceUrl = "jdbc:mysql://127.0.0.1:3306/ticket_platform"
                    + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        }

        boolean datasourceFallbackApplied = setSystemPropertyIfAbsent("spring.datasource.url", datasourceUrl);
        setSystemPropertyIfAbsent("spring.datasource.username", firstNonBlank(
                System.getenv("DATASOURCE_USERNAME"),
                System.getenv("SPRING_DATASOURCE_USERNAME"),
                System.getenv("MYSQL_USER"),
                shouldEnableDevPreviewFallback() ? "root" : null
        ));
        setSystemPropertyIfAbsent("spring.datasource.password", firstNonBlank(
                System.getenv("DATASOURCE_PASSWORD"),
                System.getenv("SPRING_DATASOURCE_PASSWORD"),
                System.getenv("MYSQL_PASSWORD"),
                System.getenv("MYSQL_ROOT_PASSWORD"),
                shouldEnableDevPreviewFallback() ? "" : null
        ));

        setSystemPropertyIfAbsent("spring.redis.host", firstNonBlank(
                System.getenv("REDIS_HOST"),
                System.getenv("SPRING_REDIS_HOST")
        ));
        setSystemPropertyIfAbsent("spring.redis.port", firstNonBlank(
                System.getenv("REDIS_PORT"),
                System.getenv("SPRING_REDIS_PORT")
        ));
        setSystemPropertyIfAbsent("spring.redis.password", firstNonBlank(
                System.getenv("REDIS_PASSWORD"),
                System.getenv("SPRING_REDIS_PASSWORD")
        ));

        setSystemPropertyIfAbsent("jwt.secret", firstNonBlank(System.getenv("JWT_SECRET")));

        if (datasourceFallbackApplied && shouldEnableDevPreviewFallback()) {
            // Why: 数据库不可达时，允许预览环境先把应用拉起，避免 CDS 直接判定启动失败
            setSystemPropertyIfAbsent("spring.datasource.hikari.initialization-fail-timeout", "0");
            setSystemPropertyIfAbsent("spring.flyway.enabled", firstNonBlank(
                    System.getenv("SPRING_FLYWAY_ENABLED"),
                    System.getenv("FLYWAY_ENABLED"),
                    "false"
            ));
            setSystemPropertyIfAbsent("management.health.db.enabled", "false");
        }
    }

    private static boolean setSystemPropertyIfAbsent(String key, String candidate) {
        if (!isBlank(System.getProperty(key)) || isBlank(candidate)) {
            return false;
        }
        System.setProperty(key, candidate);
        return true;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean shouldEnableDevPreviewFallback() {
        String activeProfiles = firstNonBlank(
                System.getProperty("spring.profiles.active"),
                System.getenv("SPRING_PROFILES_ACTIVE")
        );
        if (isBlank(activeProfiles)) {
            return true;
        }
        String normalized = activeProfiles.toLowerCase();
        if (normalized.contains("prod")) {
            return false;
        }
        return normalized.contains("dev");
    }
}
