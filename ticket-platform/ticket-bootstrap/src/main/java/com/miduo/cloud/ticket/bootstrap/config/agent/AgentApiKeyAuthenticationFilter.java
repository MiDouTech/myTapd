package com.miduo.cloud.ticket.bootstrap.config.agent;

import com.miduo.cloud.ticket.application.agent.UserApiKeyApplicationService;
import com.miduo.cloud.ticket.common.constants.AgentApiKeyConstants;
import com.miduo.cloud.ticket.common.security.SecurityUserDetails;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserApiKeyPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
 * 从 X-Api-Key 解析个人密钥并设置 SecurityContext（与 JWT 二选一或并存时优先已存在上下文）
 */
@Component
public class AgentApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AgentApiKeyAuthenticationFilter.class);

    private final UserApiKeyApplicationService userApiKeyApplicationService;
    private final UserRepository userRepository;

    public AgentApiKeyAuthenticationFilter(UserApiKeyApplicationService userApiKeyApplicationService,
                                           UserRepository userRepository) {
        this.userApiKeyApplicationService = userApiKeyApplicationService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }
            String apiKey = resolveApiKey(request);
            if (!StringUtils.hasText(apiKey)) {
                filterChain.doFilter(request, response);
                return;
            }
            SysUserApiKeyPO keyRow = userApiKeyApplicationService.validatePlaintextKey(apiKey);
            if (keyRow == null) {
                filterChain.doFilter(request, response);
                return;
            }
            User user = userRepository.findById(keyRow.getUserId());
            if (user == null || user.getAccountStatus() == null || user.getAccountStatus() != 1) {
                log.debug("API Key 对应用户不可用 userId={}", keyRow.getUserId());
                filterChain.doFilter(request, response);
                return;
            }
            List<String> roles = userRepository.findRoleCodes(user.getId());
            List<SimpleGrantedAuthority> authorities = roles != null
                    ? roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase())).collect(Collectors.toList())
                    : Collections.emptyList();

            SecurityUserDetails userDetails = new SecurityUserDetails(
                    user.getId(), user.getName(), user.getWecomUserid(), roles, authorities);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            userApiKeyApplicationService.touchLastUsed(keyRow.getId());
        } catch (Exception e) {
            log.warn("API Key 认证处理异常: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 解析个人 API 密钥：优先 {@code X-Api-Key}，其次 {@code Authorization: Bearer <mdt_...>}。
     *
     * <p>仅当 Bearer 值以 API Key 前缀（{@code mdt_}）开头时才视为密钥，避免与标准 JWT Bearer 冲突。
     */
    private String resolveApiKey(HttpServletRequest request) {
        String header = request.getHeader(AgentApiKeyConstants.HEADER_NAME);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        String authorization = request.getHeader(AgentApiKeyConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authorization) && authorization.startsWith(AgentApiKeyConstants.BEARER_PREFIX)) {
            String token = authorization.substring(AgentApiKeyConstants.BEARER_PREFIX.length()).trim();
            if (token.startsWith(AgentApiKeyConstants.KEY_PREFIX_LABEL)) {
                return token;
            }
        }
        return null;
    }
}
