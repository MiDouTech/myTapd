package com.miduo.cloud.ticket.bootstrap.config;

import com.miduo.cloud.ticket.bootstrap.config.agent.AgentApiKeyAuthenticationFilter;
import com.miduo.cloud.ticket.bootstrap.config.jwt.JwtAuthenticationFilter;
import com.miduo.cloud.ticket.bootstrap.config.jwt.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置
 * 集成企微 OAuth2.0 + JWT；未带 JWT 时可使用个人 API 密钥（X-Api-Key）
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final AgentApiKeyAuthenticationFilter agentApiKeyAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          AgentApiKeyAuthenticationFilter agentApiKeyAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.agentApiKeyAuthenticationFilter = agentApiKeyAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .and()
                .authorizeRequests()
                .antMatchers(
                        "/api/auth/**",
                        "/actuator/**",
                        "/api/actuator/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/wecom/callback/**",
                        "/api/wecom/callback/**",
                        "/api/open/ticket/**",
                        "/api/open/alert/**",
                        "/ws/**",
                        "/api/auth/sso/status",
                        "/api/auth/sso/callback",
                        "/api/auth/sso/state"
                ).permitAll()
                .anyRequest().authenticated()
                .and()
                // JWT → API Key：先尝试 JWT，未认证再尝试 X-Api-Key
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(agentApiKeyAuthenticationFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
