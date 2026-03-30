package com.miduo.cloud.ticket.bootstrap.config.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 签名密钥（至少256位，即32字节以上）
     */
    private String secret;

    /**
     * Access Token有效时间（秒），默认2小时
     */
    private long accessTokenExpire = 7200;

    /**
     * Refresh Token有效时间（秒），默认7天
     */
    private long refreshTokenExpire = 604800;

    /**
     * Token前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * HTTP请求头名称
     */
    private String headerName = "Authorization";
}
