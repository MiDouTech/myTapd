package com.miduo.cloud.ticket.bootstrap.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 插件跨域响应头兜底过滤器
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PluginCorsHeaderFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/open/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            // 为什么这里要“先写头再放行”：后面即使抛异常或返回 5xx，浏览器也能拿到可读错误，不再只显示 Failed to fetch。
            response.setHeader("Access-Control-Allow-Origin", origin.trim());
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            String requestedHeaders = request.getHeader("Access-Control-Request-Headers");
            if (StringUtils.hasText(requestedHeaders)) {
                response.setHeader("Access-Control-Allow-Headers", requestedHeaders);
            } else {
                response.setHeader("Access-Control-Allow-Headers",
                        "Authorization,Content-Type,X-App-Key,X-Timestamp,X-Nonce,X-Signature");
            }
            response.setHeader("Access-Control-Max-Age", "3600");
            appendVaryHeader(response, "Origin");
            appendVaryHeader(response, "Access-Control-Request-Headers");
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void appendVaryHeader(HttpServletResponse response, String value) {
        String current = response.getHeader("Vary");
        if (!StringUtils.hasText(current)) {
            response.setHeader("Vary", value);
            return;
        }
        if (!current.contains(value)) {
            response.setHeader("Vary", current + ", " + value);
        }
    }
}
