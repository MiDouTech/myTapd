package com.miduo.cloud.ticket.application.integration;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 开放接口已解析的接入应用凭证
 */
@Data
public class ResolvedIntegrationClient implements Serializable {

    private String appKey;

    private String appSecret;

    private String appName;

    private Long integrationAppId;

    private String systemCode;

    private Long defaultCategoryId;

    private String categoryMappingJson;

    private String callbackUrl;

    private String callbackSecret;

    private String allowedOrigins;

    private Set<String> permissions;

    private boolean fromDatabase;
}
