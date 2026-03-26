package com.miduo.cloud.ticket.entity.dto.sso;

import lombok.Data;

import java.io.Serializable;

/**
 * SSO 状态输出：返回 SSO 是否启用以及登录桥 URL 等信息
 */
@Data
public class SsoStatusOutput implements Serializable {

    /**
     * SSO 是否启用
     */
    private boolean enabled;

    /**
     * 落地页 URL（米多跳转的目标地址）
     */
    private String callbackUrl;

    /**
     * 应用编码
     */
    private String appCode;
}
