package com.miduo.cloud.ticket.bootstrap.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * JWT Token签发与校验
 * 双Token机制：AccessToken(2h) + RefreshToken(7d)
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private Key signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes();
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成Access Token
     */
    public String createAccessToken(Long userId, String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpire() * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成Refresh Token
     */
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshTokenExpire() * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析Token获取Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 校验Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /**
     * 从Token提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /**
     * 从Token提取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_ROLES, List.class);
    }

    /**
     * 判断是否是RefreshToken
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否是AccessToken
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取AccessToken有效时间（秒）
     */
    public long getAccessTokenExpireSeconds() {
        return jwtProperties.getAccessTokenExpire();
    }
}
