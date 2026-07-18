package com.miduo.cloud.ticket.application.webhook;

import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po.WebhookConfigPO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 判断工单分类是否命中 Webhook 配置的适用分类范围。
 * category_ids 为空表示全部分类（兼容历史配置）。
 */
@Component
public class WebhookCategoryScopeMatcher {

    private final TicketCategoryMapper ticketCategoryMapper;

    public WebhookCategoryScopeMatcher(TicketCategoryMapper ticketCategoryMapper) {
        this.ticketCategoryMapper = ticketCategoryMapper;
    }

    public boolean matches(WebhookConfigPO config, Long ticketCategoryId) {
        if (config == null) {
            return false;
        }
        Set<Long> allowedCategoryIds = parseCategoryIds(config.getCategoryIds());
        if (allowedCategoryIds.isEmpty()) {
            return true;
        }
        if (ticketCategoryId == null) {
            return false;
        }
        if (allowedCategoryIds.contains(ticketCategoryId)) {
            return true;
        }
        if (isIncludeDescendantsEnabled(config)) {
            return isDescendantOfAnyAllowed(ticketCategoryId, allowedCategoryIds);
        }
        return false;
    }

    public Set<Long> parseCategoryIds(String rawCategoryIds) {
        if (rawCategoryIds == null || rawCategoryIds.trim().isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        String[] parts = rawCategoryIds.split("[,，]");
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String normalized = part.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.parseLong(normalized));
            } catch (NumberFormatException ignored) {
                // 忽略非法片段，由配置保存阶段校验
            }
        }
        return result;
    }

    private boolean isIncludeDescendantsEnabled(WebhookConfigPO config) {
        return config.getIncludeDescendants() != null && config.getIncludeDescendants() == 1;
    }

    private boolean isDescendantOfAnyAllowed(Long ticketCategoryId, Set<Long> allowedCategoryIds) {
        Long currentId = ticketCategoryId;
        Set<Long> visited = new HashSet<>();
        while (currentId != null && visited.add(currentId)) {
            TicketCategoryPO category = ticketCategoryMapper.selectById(currentId);
            if (category == null) {
                break;
            }
            Long parentId = category.getParentId();
            if (parentId != null && allowedCategoryIds.contains(parentId)) {
                return true;
            }
            currentId = parentId;
        }
        return false;
    }
}
