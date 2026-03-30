package com.miduo.cloud.ticket.entity.dto.auth;

import javax.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 临时登录请求（仅 dev-login.enabled=true 时可用）
 * 支持两种方式：
 * 1. 管理员账号：username + password（由 Nacos 配置注入）
 * 2. 手机号登录：username=手机号, password（由 Nacos 配置注入）
 */
@Data
public class DevLoginInput implements Serializable {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
