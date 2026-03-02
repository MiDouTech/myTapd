package com.miduo.cloud.ticket.application.auth;

import java.util.List;

/**
 * Token服务接口
 * 由bootstrap模块的JwtTokenProvider实现，解决application层不依赖bootstrap的问题
 */
public interface TokenService {

    /**
     * 创建AccessToken
     */
    String createAccessToken(Long userId, String username, List<String> roles);

    /**
     * 创建RefreshToken
     */
    String createRefreshToken(Long userId);

    /**
     * 校验RefreshToken，返回用户ID
     */
    Long validateRefreshToken(String refreshToken);

    /**
     * 获取AccessToken有效时间（秒）
     */
    long getAccessTokenExpireSeconds();
}
