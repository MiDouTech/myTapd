package com.miduo.cloud.ticket.entity.dto.auth;

import javax.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 测试环境硬编码登录请求（仅 dev-login.enabled=true 时可用）
 */
@Data
public class DevLoginInput implements Serializable {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
