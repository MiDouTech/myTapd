package com.miduo.cloud.ticket.entity.dto.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 外部用户本地登录请求（手机号 + 密码）
 * 接口编号：API000432
 * 产品文档功能：外部上下游人员专属登录（无企微账号）
 */
@Data
public class LocalLoginInput implements Serializable {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}
