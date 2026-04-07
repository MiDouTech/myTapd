package com.miduo.cloud.ticket.infrastructure.external.qiniu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 七牛云存储配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "qiniu")
public class QiniuProperties {

    /**
     * AccessKey
     */
    private String accessKey;

    /**
     * SecretKey
     */
    private String secretKey;

    /**
     * 存储空间名称（Bucket）
     */
    private String bucket;

    /**
     * 文件访问域名（含协议前缀，如 https://cdn.example.com）
     */
    private String domain;

    /**
     * 存储区域（默认 z2 华南）
     * z0=华东、z1=华北、z2=华南、na0=北美、as0=东南亚
     */
    private String zone = "z2";

    /**
     * 上传 Token 有效期（秒，默认 3600）
     */
    private Long tokenExpireSeconds = 3600L;

    /**
     * 图片上传路径前缀
     */
    private String imagePathPrefix = "ticket/images/";

    /**
     * 视频上传路径前缀
     */
    private String videoPathPrefix = "ticket/videos/";

    /**
     * 允许上传的最大文件大小（MB，默认 10MB）
     */
    private Long maxFileSizeMb = 10L;
}
