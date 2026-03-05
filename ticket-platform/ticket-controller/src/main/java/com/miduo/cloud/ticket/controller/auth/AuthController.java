package com.miduo.cloud.ticket.controller.auth;

import com.miduo.cloud.ticket.application.auth.AuthApplicationService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.entity.dto.auth.DevLoginInput;
import com.miduo.cloud.ticket.entity.dto.auth.LoginOutput;
import com.miduo.cloud.ticket.entity.dto.auth.RefreshTokenInput;
import com.miduo.cloud.ticket.entity.dto.auth.WecomLoginInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证接口
 */
@Tag(name = "认证管理", description = "企微登录、Token刷新")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 企微扫码登录
     * 接口编号：API000400
     * 产品文档功能：4.6.1 企微扫码登录
     */
    @Operation(summary = "企微扫码登录", description = "接口编号：API000400。用企微授权code换取JWT Token")
    @PostMapping("/wecom/login")
    public ApiResult<LoginOutput> wecomLogin(@Valid @RequestBody WecomLoginInput input) {
        LoginOutput output = authApplicationService.wecomLogin(input);
        return ApiResult.success(output);
    }

    /**
     * 刷新Token
     * 接口编号：API000401
     * 产品文档功能：4.6.1 Token管理（双Token机制）
     */
    @Operation(summary = "刷新Token", description = "接口编号：API000401。用RefreshToken换取新的AccessToken")
    @PostMapping("/refresh")
    public ApiResult<LoginOutput> refreshToken(@Valid @RequestBody RefreshTokenInput input) {
        LoginOutput output = authApplicationService.refreshToken(input);
        return ApiResult.success(output);
    }

    /**
     * 测试环境硬编码账号登录
     * 接口编号：API000402
     * 产品文档功能：测试环境专用（dev-login.enabled=true 时生效）
     */
    @Operation(summary = "测试账号登录", description = "接口编号：API000402。仅在 dev-login.enabled=true 时可用，生产环境禁止开启")
    @PostMapping("/dev/login")
    public ApiResult<LoginOutput> devLogin(@Valid @RequestBody DevLoginInput input) {
        LoginOutput output = authApplicationService.devLogin(input);
        return ApiResult.success(output);
    }
}
