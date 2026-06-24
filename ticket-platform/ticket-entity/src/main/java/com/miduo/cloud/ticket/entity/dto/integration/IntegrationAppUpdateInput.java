package com.miduo.cloud.ticket.entity.dto.integration;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

/**
 * 更新接入应用
 * 接口编号：API000529
 */
@Data
public class IntegrationAppUpdateInput implements Serializable {

    @NotBlank(message = "应用名称不能为空")
    private String appName;

    @NotNull(message = "默认分类不能为空")
    private Long defaultCategoryId;

    private Map<String, Long> categoryMapping;

    private String callbackUrl;

    private String callbackSecret;

    private String allowedOrigins;

    private String permissions;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
