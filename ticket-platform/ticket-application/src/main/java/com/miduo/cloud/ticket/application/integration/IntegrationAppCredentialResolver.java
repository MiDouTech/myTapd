package com.miduo.cloud.ticket.application.integration;

import com.miduo.cloud.ticket.common.enums.PluginPermission;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.mapper.IntegrationAppMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po.IntegrationAppPO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 接入应用凭证解析（DB）
 */
@Service
public class IntegrationAppCredentialResolver {

    private final IntegrationAppMapper integrationAppMapper;

    public IntegrationAppCredentialResolver(IntegrationAppMapper integrationAppMapper) {
        this.integrationAppMapper = integrationAppMapper;
    }

    public ResolvedIntegrationClient resolve(String appKey) {
        if (!StringUtils.hasText(appKey)) {
            return null;
        }
        IntegrationAppPO app = integrationAppMapper.selectByAppKey(appKey.trim());
        if (app == null || app.getStatus() == null || app.getStatus() != 1) {
            return null;
        }
        ResolvedIntegrationClient client = new ResolvedIntegrationClient();
        client.setAppKey(app.getAppKey());
        client.setAppSecret(app.getAppSecret());
        client.setAppName(app.getAppName());
        client.setIntegrationAppId(app.getId());
        client.setSystemCode(app.getSystemCode());
        client.setDefaultCategoryId(app.getDefaultCategoryId());
        client.setCategoryMappingJson(app.getCategoryMapping());
        client.setCallbackUrl(app.getCallbackUrl());
        client.setCallbackSecret(app.getCallbackSecret());
        client.setAllowedOrigins(app.getAllowedOrigins());
        client.setPermissions(PluginPermission.parsePermissionSet(app.getPermissions()));
        client.setFromDatabase(true);
        return client;
    }

    public IntegrationAppPO requireEnabledApp(Long integrationAppId) {
        if (integrationAppId == null) {
            return null;
        }
        IntegrationAppPO app = integrationAppMapper.selectById(integrationAppId);
        if (app == null || app.getStatus() == null || app.getStatus() != 1) {
            return null;
        }
        return app;
    }

    public boolean isOriginAllowed(ResolvedIntegrationClient client, String origin) {
        if (client == null) {
            return false;
        }
        if (!StringUtils.hasText(client.getAllowedOrigins())) {
            return true;
        }
        if (!StringUtils.hasText(origin)) {
            return false;
        }
        String normalizedOrigin = origin.trim();
        for (String item : client.getAllowedOrigins().split(",")) {
            if (item != null && normalizedOrigin.equalsIgnoreCase(item.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPermission(ResolvedIntegrationClient client, PluginPermission permission) {
        if (client == null || permission == null) {
            return false;
        }
        return PluginPermission.hasPermission(client.getPermissions(), permission);
    }
}
