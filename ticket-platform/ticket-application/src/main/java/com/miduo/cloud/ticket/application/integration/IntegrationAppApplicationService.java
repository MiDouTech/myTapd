package com.miduo.cloud.ticket.application.integration;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.PluginPermission;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppCreateInput;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppOutput;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppPageInput;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppRotateSecretOutput;
import com.miduo.cloud.ticket.entity.dto.integration.IntegrationAppUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.mapper.IntegrationAppMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po.IntegrationAppPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 接入应用管理
 */
@Service
public class IntegrationAppApplicationService extends BaseApplicationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final IntegrationAppMapper integrationAppMapper;
    private final TicketCategoryMapper ticketCategoryMapper;

    public IntegrationAppApplicationService(IntegrationAppMapper integrationAppMapper,
                                              TicketCategoryMapper ticketCategoryMapper) {
        this.integrationAppMapper = integrationAppMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
    }

    public PageOutput<IntegrationAppOutput> page(IntegrationAppPageInput input) {
        Page<IntegrationAppPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        QueryWrapper<IntegrationAppPO> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(input.getKeyword())) {
            String keyword = input.getKeyword().trim();
            wrapper.and(w -> w.like("app_name", keyword)
                    .or().like("system_code", keyword)
                    .or().like("app_key", keyword));
        }
        if (input.getStatus() != null) {
            wrapper.eq("status", input.getStatus());
        }
        wrapper.orderByDesc("create_time");
        Page<IntegrationAppPO> result = integrationAppMapper.selectPage(page, wrapper);
        List<IntegrationAppOutput> records = result.getRecords().stream()
                .map(this::toOutput)
                .collect(Collectors.toList());
        return PageOutput.of(records, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    public IntegrationAppOutput detail(Long id) {
        IntegrationAppPO po = requireApp(id);
        return toOutput(po);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(IntegrationAppCreateInput input) {
        validateCategory(input.getDefaultCategoryId());
        validateSystemCodeUnique(input.getSystemCode(), null);
        IntegrationAppPO po = new IntegrationAppPO();
        po.setAppName(input.getAppName().trim());
        po.setSystemCode(input.getSystemCode().trim());
        po.setAppKey(generateAppKey());
        po.setAppSecret(generateAppSecret());
        po.setDefaultCategoryId(input.getDefaultCategoryId());
        po.setCategoryMapping(serializeCategoryMapping(input.getCategoryMapping()));
        po.setCallbackUrl(trimToNull(input.getCallbackUrl()));
        po.setCallbackSecret(trimToNull(input.getCallbackSecret()));
        po.setAllowedOrigins(trimToNull(input.getAllowedOrigins()));
        po.setPermissions(normalizePermissions(input.getPermissions()));
        po.setStatus(1);
        integrationAppMapper.insert(po);
        return po.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, IntegrationAppUpdateInput input) {
        IntegrationAppPO existing = requireApp(id);
        validateCategory(input.getDefaultCategoryId());
        existing.setAppName(input.getAppName().trim());
        existing.setDefaultCategoryId(input.getDefaultCategoryId());
        existing.setCategoryMapping(serializeCategoryMapping(input.getCategoryMapping()));
        existing.setCallbackUrl(trimToNull(input.getCallbackUrl()));
        if (StringUtils.hasText(input.getCallbackSecret())) {
            existing.setCallbackSecret(input.getCallbackSecret().trim());
        }
        existing.setAllowedOrigins(trimToNull(input.getAllowedOrigins()));
        existing.setPermissions(normalizePermissions(input.getPermissions()));
        existing.setStatus(input.getStatus());
        integrationAppMapper.updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public IntegrationAppRotateSecretOutput rotateSecret(Long id) {
        IntegrationAppPO existing = requireApp(id);
        existing.setAppSecret(generateAppSecret());
        integrationAppMapper.updateById(existing);
        IntegrationAppRotateSecretOutput output = new IntegrationAppRotateSecretOutput();
        output.setAppKey(existing.getAppKey());
        output.setAppSecret(existing.getAppSecret());
        return output;
    }

    private IntegrationAppPO requireApp(Long id) {
        IntegrationAppPO po = integrationAppMapper.selectById(id);
        if (po == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "接入应用不存在");
        }
        return po;
    }

    private void validateCategory(Long categoryId) {
        TicketCategoryPO category = ticketCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "默认工单分类不存在");
        }
    }

    private void validateSystemCodeUnique(String systemCode, Long excludeId) {
        IntegrationAppPO existing = integrationAppMapper.selectBySystemCode(systemCode.trim());
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "系统标识已存在: " + systemCode);
        }
    }

    private String normalizePermissions(String permissions) {
        if (!StringUtils.hasText(permissions)) {
            return PluginPermission.joinDefaultPermissions();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String item : permissions.split(",")) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String code = item.trim();
            if (!PluginPermission.isValidPermission(code)) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "不支持的权限: " + code);
            }
            normalized.add(code);
        }
        if (normalized.isEmpty()) {
            return PluginPermission.joinDefaultPermissions();
        }
        return String.join(",", normalized);
    }

    private String serializeCategoryMapping(Map<String, Long> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(mapping);
    }

    private IntegrationAppOutput toOutput(IntegrationAppPO po) {
        IntegrationAppOutput output = new IntegrationAppOutput();
        output.setId(po.getId());
        output.setAppName(po.getAppName());
        output.setAppKey(po.getAppKey());
        output.setAppSecret(po.getAppSecret());
        output.setSystemCode(po.getSystemCode());
        output.setDefaultCategoryId(po.getDefaultCategoryId());
        if (StringUtils.hasText(po.getCategoryMapping())) {
            output.setCategoryMapping(JSON.parseObject(po.getCategoryMapping(),
                    new TypeReference<Map<String, Long>>() {
                    }));
        }
        output.setCallbackUrl(po.getCallbackUrl());
        output.setAllowedOrigins(po.getAllowedOrigins());
        output.setPermissions(po.getPermissions());
        output.setStatus(po.getStatus());
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());
        return output;
    }

    private String generateAppKey() {
        return "ak_" + randomHex(16);
    }

    private String generateAppSecret() {
        return "as_" + randomHex(32);
    }

    private String randomHex(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
