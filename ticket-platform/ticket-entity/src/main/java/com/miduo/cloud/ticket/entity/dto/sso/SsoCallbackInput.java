package com.miduo.cloud.ticket.entity.dto.sso;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * SSO 回调输入：前端将 exchange token 提交到后端
 */
@Data
public class SsoCallbackInput implements Serializable {

    @NotBlank(message = "exchange token 不能为空")
    private String token;

    /**
     * CSRF 防护状态参数
     */
    private String state;
}
