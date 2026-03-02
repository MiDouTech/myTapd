package com.miduo.cloud.ticket.application.webhook;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.WebhookEventType;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookConfigCreateInput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookConfigOutput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookConfigPageInput;
import com.miduo.cloud.ticket.entity.dto.webhook.WebhookConfigUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.mapper.WebhookConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po.WebhookConfigPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Webhook配置应用服务
 */
@Service
public class WebhookConfigApplicationService extends BaseApplicationService {

    private final WebhookConfigMapper webhookConfigMapper;

    public WebhookConfigApplicationService(WebhookConfigMapper webhookConfigMapper) {
        this.webhookConfigMapper = webhookConfigMapper;
    }

    public PageOutput<WebhookConfigOutput> page(WebhookConfigPageInput input) {
        Page<WebhookConfigPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        QueryWrapper<WebhookConfigPO> wrapper = new QueryWrapper<>();
        buildPageCondition(wrapper, input);
        wrapper.orderByDesc("create_time");

        Page<WebhookConfigPO> result = webhookConfigMapper.selectPage(page, wrapper);
        List<WebhookConfigOutput> outputs = result.getRecords().stream()
                .map(this::convertToOutput)
                .collect(Collectors.toList());
        return PageOutput.of(outputs, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    public WebhookConfigOutput detail(Long id) {
        WebhookConfigPO po = webhookConfigMapper.selectById(id);
        if (po == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "Webhook配置不存在");
        }
        return convertToOutput(po);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(WebhookConfigCreateInput input) {
        WebhookConfigPO po = new WebhookConfigPO();
        fillForCreateOrUpdate(po, input.getUrl(), input.getSecret(), input.getEventTypes(),
                input.getIsActive(), input.getTimeoutMs(), input.getMaxRetryTimes(), input.getDescription());
        webhookConfigMapper.insert(po);
        return po.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WebhookConfigUpdateInput input) {
        WebhookConfigPO existing = webhookConfigMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "Webhook配置不存在");
        }
        fillForCreateOrUpdate(existing, input.getUrl(), input.getSecret(), input.getEventTypes(),
                input.getIsActive(), input.getTimeoutMs(), input.getMaxRetryTimes(), input.getDescription());
        webhookConfigMapper.updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WebhookConfigPO existing = webhookConfigMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "Webhook配置不存在");
        }
        webhookConfigMapper.deleteById(id);
    }

    private void buildPageCondition(QueryWrapper<WebhookConfigPO> wrapper, WebhookConfigPageInput input) {
        if (input.getKeyword() != null && !input.getKeyword().trim().isEmpty()) {
            String keyword = input.getKeyword().trim();
            wrapper.and(w -> w.like("url", keyword).or().like("description", keyword));
        }
        if (input.getEventType() != null && !input.getEventType().trim().isEmpty()) {
            wrapper.apply("FIND_IN_SET({0}, event_types)", input.getEventType().trim());
        }
        if (input.getIsActive() != null) {
            wrapper.eq("is_active", input.getIsActive());
        }
    }

    private void fillForCreateOrUpdate(WebhookConfigPO po,
                                       String url,
                                       String secret,
                                       List<String> eventTypes,
                                       Integer isActive,
                                       Integer timeoutMs,
                                       Integer maxRetryTimes,
                                       String description) {
        po.setUrl(url);
        po.setSecret(secret);
        po.setEventTypes(joinEventTypes(eventTypes));
        po.setIsActive(isActive);
        po.setTimeoutMs(timeoutMs);
        po.setMaxRetryTimes(maxRetryTimes);
        po.setDescription(description);
    }

    private String joinEventTypes(List<String> eventTypes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "事件类型不能为空");
        }
        LinkedHashSet<String> typeSet = new LinkedHashSet<>();
        for (String eventType : eventTypes) {
            String code = eventType == null ? null : eventType.trim();
            if (code == null || code.isEmpty()) {
                continue;
            }
            if (WebhookEventType.fromCode(code) == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "不支持的事件类型: " + code);
            }
            typeSet.add(code);
        }
        if (typeSet.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "事件类型不能为空");
        }
        return String.join(",", typeSet);
    }

    private List<String> splitEventTypes(String eventTypes) {
        if (eventTypes == null || eventTypes.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] array = eventTypes.split(",");
        List<String> result = new ArrayList<>();
        for (String item : array) {
            if (item != null && !item.trim().isEmpty()) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private WebhookConfigOutput convertToOutput(WebhookConfigPO po) {
        WebhookConfigOutput output = new WebhookConfigOutput();
        output.setId(po.getId());
        output.setUrl(po.getUrl());
        output.setSecret(po.getSecret());
        output.setEventTypes(splitEventTypes(po.getEventTypes()));
        output.setIsActive(po.getIsActive());
        output.setTimeoutMs(po.getTimeoutMs());
        output.setMaxRetryTimes(po.getMaxRetryTimes());
        output.setDescription(po.getDescription());
        output.setLastSuccessTime(po.getLastSuccessTime());
        output.setLastFailTime(po.getLastFailTime());
        output.setLastFailReason(po.getLastFailReason());
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());
        return output;
    }
}
