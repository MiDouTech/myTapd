package com.miduo.cloud.ticket.application.plugin;

import com.alibaba.fastjson2.JSON;
import com.miduo.cloud.ticket.application.integration.IntegrationAppCredentialResolver;
import com.miduo.cloud.ticket.application.integration.ResolvedIntegrationClient;
import com.miduo.cloud.ticket.application.template.TemplateApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginCategoryConfigOutput;
import com.miduo.cloud.ticket.entity.dto.plugin.PluginConfigOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateListOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po.IntegrationAppPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件 SDK 配置
 */
@Service
public class PluginConfigApplicationService {

    private final IntegrationAppCredentialResolver credentialResolver;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final TemplateApplicationService templateApplicationService;

    public PluginConfigApplicationService(IntegrationAppCredentialResolver credentialResolver,
                                          TicketCategoryMapper ticketCategoryMapper,
                                          TemplateApplicationService templateApplicationService) {
        this.credentialResolver = credentialResolver;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.templateApplicationService = templateApplicationService;
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
        IntegrationAppPO app = credentialResolver.requireEnabledApp(client.getIntegrationAppId());
        List<Long> categoryIds = StringUtils.hasText(app.getCategoryIds())
                ? JSON.parseArray(app.getCategoryIds(), Long.class)
                : Collections.singletonList(app.getDefaultCategoryId());
        List<PluginCategoryConfigOutput> categories = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            TicketCategoryPO category = ticketCategoryMapper.selectById(categoryId);
            if (category == null || !Integer.valueOf(1).equals(category.getIsActive())) {
                continue;
            }
            List<TemplateListOutput> templates = templateApplicationService.getTemplateList(categoryId);
            TemplateListOutput template = templates.isEmpty() ? null : templates.get(0);
            PluginCategoryConfigOutput item = new PluginCategoryConfigOutput();
            item.setId(category.getId());
            item.setName(category.getName());
            if (template != null) {
                item.setTemplateId(template.getId());
                item.setTemplateName(template.getName());
                item.setFieldsConfig(template.getFieldsConfig());
            }
            categories.add(item);
        }
        output.setCategories(categories);
        return output;
    }
}
