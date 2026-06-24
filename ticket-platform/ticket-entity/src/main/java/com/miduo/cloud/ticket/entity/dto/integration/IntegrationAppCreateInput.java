package com.miduo.cloud.ticket.entity.dto.integration;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

/**
 * 创建接入应用
 * 接口编号：API000528
 */
@Data
public class IntegrationAppCreateInput implements Serializable {

    @NotBlank(message = "应用名称不能为空")
    private String appName;

    @NotBlank(message = "系统标识不能为空")
    private String systemCode;

    @NotNull(message = "默认分类不能为空")
    private Long defaultCategoryId;

    private Map<String, Long> categoryMapping;

    private String callbackUrl;

    private String callbackSecret;

    private String allowedOrigins;

    private String permissions;
}
