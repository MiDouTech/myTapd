package com.miduo.cloud.ticket.bootstrap.config.jwt;

import com.alibaba.fastjson2.JSON;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 未认证访问处理器
 * 未登录或Token无效时返回401
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResult<Void> result = ApiResult.fail(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
