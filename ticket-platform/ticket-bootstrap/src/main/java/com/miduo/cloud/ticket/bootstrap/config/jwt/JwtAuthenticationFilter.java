package com.miduo.cloud.ticket.bootstrap.config.jwt;

import com.miduo.cloud.ticket.common.security.SecurityUserDetails;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 * 从请求头提取JWT Token，校验后设置SecurityContext
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   JwtProperties jwtProperties,
                                   UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null && jwtTokenProvider.isAccessToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userRepository.findById(userId);
                    if (user != null && user.getAccountStatus() != null && user.getAccountStatus() == 1) {
                        List<SimpleGrantedAuthority> authorities = roles != null
                                ? roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase())).collect(Collectors.toList())
                                : Collections.emptyList();

                        SecurityUserDetails userDetails = new SecurityUserDetails(
                                user.getId(), user.getName(), user.getWecomUserid(), roles, authorities);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            log.debug("JWT Token已过期: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT认证处理异常: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeaderName());
        if (header != null && header.startsWith(jwtProperties.getTokenPrefix())) {
            return header.substring(jwtProperties.getTokenPrefix().length());
        }
        return null;
    }
}
