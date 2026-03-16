package com.miduo.cloud.ticket.entity.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;

/**
 * 测试环境硬编码账号登录请求
 * 仅在 dev-login.enabled=true 时可用
 */
@Schema(description = "测试账号登录请求")
public class DevLoginInput {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "测试账号用户名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "测试账号密码", example = "admin2026")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
