package com.miduo.cloud.ticket.application.plugin;

import com.miduo.cloud.ticket.application.integration.IntegrationAppCredentialResolver;
import com.miduo.cloud.ticket.application.integration.ResolvedIntegrationClient;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginConfigOutput;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件 SDK 配置
 */
@Service
public class PluginConfigApplicationService {

    private final IntegrationAppCredentialResolver credentialResolver;

    public PluginConfigApplicationService(IntegrationAppCredentialResolver credentialResolver) {
        this.credentialResolver = credentialResolver;
    }

    public PluginConfigOutput getConfig(String appKey, String origin) {
        ResolvedIntegrationClient client = credentialResolver.resolve(appKey);
        if (client == null) {
            throw BusinessException.of(ErrorCode.OPEN_API_APP_NOT_FOUND, "接入应用不存在或已禁用");
        }
        if (!credentialResolver.isOriginAllowed(client, origin)) {
            throw BusinessException.of(ErrorCode.PLUGIN_ORIGIN_DENIED, "来源域名未授权");
        }
        PluginConfigOutput output = new PluginConfigOutput();
        output.setAppName(client.getAppName());
        output.setSystemCode(client.getSystemCode());
        output.setDefaultPriority(Priority.MEDIUM.getCode());
        output.setShowPriorityPicker(Boolean.TRUE);
        Map<String, Object> theme = new HashMap<>();
        theme.put("primaryColor", "#1675d1");
        output.setTheme(theme);
        return output;
    }
}
