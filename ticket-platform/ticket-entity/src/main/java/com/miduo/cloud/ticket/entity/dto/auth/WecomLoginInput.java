package com.miduo.cloud.ticket.entity.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 企微扫码登录请求
 */
@Data
public class WecomLoginInput implements Serializable {

    @NotBlank(message = "授权码不能为空")
    private String code;
}
