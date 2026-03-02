package com.miduo.cloud.ticket.bootstrap.config.jwt;

import com.miduo.cloud.ticket.application.auth.TokenService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Token服务实现
 * 桥接application层TokenService接口与JwtTokenProvider
 */
@Service
public class TokenServiceImpl implements TokenService {

    private final JwtTokenProvider jwtTokenProvider;

    public TokenServiceImpl(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public String createAccessToken(Long userId, String username, List<String> roles) {
        return jwtTokenProvider.createAccessToken(userId, username, roles);
    }

    @Override
    public String createRefreshToken(Long userId) {
        return jwtTokenProvider.createRefreshToken(userId);
    }

    @Override
    public Long validateRefreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return null;
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            return null;
        }
        return jwtTokenProvider.getUserIdFromToken(refreshToken);
    }

    @Override
    public long getAccessTokenExpireSeconds() {
        return jwtTokenProvider.getAccessTokenExpireSeconds();
    }
}
