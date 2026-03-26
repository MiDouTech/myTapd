package com.miduo.cloud.ticket.controller.sso;

import com.miduo.cloud.ticket.application.sso.SsoApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.auth.LoginOutput;
import com.miduo.cloud.ticket.entity.dto.sso.SsoCallbackInput;
import com.miduo.cloud.ticket.entity.dto.sso.SsoStatusOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

/**
 * 米多星球 SSO 认证接口
 */
@Tag(name = "SSO认证管理", description = "米多星球SSO单点登录集成")
@RestController
@RequestMapping("/api/auth/sso")
public class SsoAuthController {

    private final SsoApplicationService ssoApplicationService;

    public SsoAuthController(SsoApplicationService ssoApplicationService) {
        this.ssoApplicationService = ssoApplicationService;
    }

    /**
     * 查询 SSO 状态
     * 接口编号：API000410
     * 产品文档功能：SSO 登录 - 获取 SSO 配置状态
     */
    @Operation(summary = "查询SSO状态", description = "接口编号：API000410。返回SSO是否启用及相关配置")
    @GetMapping("/status")
    public ApiResult<SsoStatusOutput> getSsoStatus() {
        SsoStatusOutput output = ssoApplicationService.getSsoStatus();
        return ApiResult.success(output);
    }

    /**
     * 生成 state 参数
     * 接口编号：API000411
     * 产品文档功能：SSO 登录 - 生成 CSRF 防护 state
     */
    @Operation(summary = "生成SSO state参数", description = "接口编号：API000411。生成 state 用于 CSRF 防护")
    @PostMapping("/state")
    public ApiResult<Map<String, String>> generateState() {
        String state = ssoApplicationService.generateState();
        return ApiResult.success(Collections.singletonMap("state", state));
    }

    /**
     * SSO 回调：校验 exchange token 并返回本地 JWT
     * 接口编号：API000412
     * 产品文档功能：SSO 登录 - 第三方回调校验
     */
    @Operation(summary = "SSO回调登录", description = "接口编号：API000412。校验米多exchange token，返回本地JWT")
    @PostMapping("/callback")
    public ApiResult<LoginOutput> ssoCallback(@Valid @RequestBody SsoCallbackInput input,
                                               HttpServletRequest request) {
        LoginOutput output = ssoApplicationService.ssoCallback(
                input, getClientIp(request), getUserAgent(request));
        return ApiResult.success(output);
    }

    /**
     * SSO 登出：吊销米多 sessionToken
     * 接口编号：API000413
     * 产品文档功能：SSO 登录 - 登出并吊销米多会话
     */
    @Operation(summary = "SSO登出", description = "接口编号：API000413。吊销米多sessionToken并清理本地SSO会话")
    @PostMapping("/logout")
    public ApiResult<Void> ssoLogout(HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        ssoApplicationService.ssoLogout(userId, getClientIp(request), getUserAgent(request));
        return ApiResult.success();
    }

    /**
     * 获取登录桥 URL
     * 接口编号：API000414
     * 产品文档功能：SSO 登录 - 会话失效后重新获取身份
     */
    @Operation(summary = "获取登录桥URL", description = "接口编号：API000414。会话失效时通过登录桥重新获取米多身份")
    @GetMapping("/bridge-url")
    public ApiResult<Map<String, String>> getBridgeUrl() {
        String url = ssoApplicationService.getBridgeUrl();
        return ApiResult.success(Collections.singletonMap("bridgeUrl", url));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "";
    }

    private String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "";
    }
}
