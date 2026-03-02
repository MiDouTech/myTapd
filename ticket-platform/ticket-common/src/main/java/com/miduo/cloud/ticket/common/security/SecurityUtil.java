package com.miduo.cloud.ticket.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

/**
 * Security工具类
 * 从SecurityContext获取当前登录用户信息
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 获取当前登录用户详情
     */
    public static SecurityUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUserDetails) {
            return (SecurityUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        SecurityUserDetails user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        SecurityUserDetails user = getCurrentUser();
        return user != null ? user.getName() : "system";
    }

    /**
     * 获取当前登录用户角色列表
     */
    public static List<String> getCurrentUserRoles() {
        SecurityUserDetails user = getCurrentUser();
        return user != null ? user.getRoleCodes() : Collections.emptyList();
    }

    /**
     * 判断当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }
}
