package com.miduo.cloud.ticket.entity.dto.auth;

import javax.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新Token请求
 */
@Data
public class RefreshTokenInput implements Serializable {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
