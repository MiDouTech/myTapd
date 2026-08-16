package com.miduo.cloud.ticket.application.template;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.template.TemplateAdminListOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateCreateInput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateCustomFieldInput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateDetailOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateFieldOptionOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateFieldOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateListOutput;
import com.miduo.cloud.ticket.entity.dto.template.TemplateUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.CustomFieldDefinitionMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketTemplateCustomFieldMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketTemplateMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.CustomFieldDefinitionPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketTemplateCustomFieldPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketTemplatePO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TemplateApplicationService {

    private static final String SYSTEM_DEFAULT_CODE = "system_default";

    private final TicketTemplateMapper templateMapper;
    private final TicketTemplateCustomFieldMapper templateFieldMapper;
    private final CustomFieldDefinitionMapper customFieldMapper;
    private final TicketCategoryMapper categoryMapper;
    private final TicketMapper ticketMapper;

    public TemplateApplicationService(TicketTemplateMapper templateMapper,
                                      TicketTemplateCustomFieldMapper templateFieldMapper,
                                      CustomFieldDefinitionMapper customFieldMapper,
                                      TicketCategoryMapper categoryMapper,
                                      TicketMapper ticketMapper) {
        this.templateMapper = templateMapper;
        this.templateFieldMapper = templateFieldMapper;
        this.customFieldMapper = customFieldMapper;
        this.categoryMapper = categoryMapper;
        this.ticketMapper = ticketMapper;
    }

    public List<TemplateListOutput> getTemplateList(Long categoryId) {
        LambdaQueryWrapper<TicketTemplatePO> query = new LambdaQueryWrapper<TicketTemplatePO>()
                .eq(TicketTemplatePO::getIsActive, 1);
        if (categoryId != null) {
            TicketCategoryPO category = categoryMapper.selectById(categoryId);
            if (category != null && category.getTemplateId() != null) {
                query.and(wrapper -> wrapper.eq(TicketTemplatePO::getId, category.getTemplateId())
                        .or().eq(TicketTemplatePO::getCategoryId, categoryId));
            } else {
                query.eq(TicketTemplatePO::getCategoryId, categoryId);
            }
        }
        return templateMapper.selectList(query.orderByAsc(TicketTemplatePO::getSortOrder)
                        .orderByDesc(TicketTemplatePO::getCreateTime))
                .stream().map(this::toLegacyOutput).collect(Collectors.toList());
    }

    public List<TemplateAdminListOutput> getAdminList(String keyword, Integer isActive) {
        LambdaQueryWrapper<TicketTemplatePO> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String normalized = keyword.trim();
            query.and(wrapper -> wrapper.like(TicketTemplatePO::getName, normalized)
                    .or().like(TicketTemplatePO::getDescription, normalized)
                    .or().like(TicketTemplatePO::getTemplateCode, normalized));
        }
        if (isActive != null) {
            query.eq(TicketTemplatePO::getIsActive, normalizeFlag(isActive));
        }
        return templateMapper.selectList(query.orderByDesc(TicketTemplatePO::getIsSystem)
                        .orderByAsc(TicketTemplatePO::getSortOrder)
                        .orderByDesc(TicketTemplatePO::getUpdateTime))
                .stream().map(this::toAdminOutput).collect(Collectors.toList());
    }

    public TemplateDetailOutput getDetail(Long id) {
        TicketTemplatePO template = requireTemplate(id);
        List<TicketTemplateCustomFieldPO> relations = templateFieldMapper.selectList(
                new LambdaQueryWrapper<TicketTemplateCustomFieldPO>()
                        .eq(TicketTemplateCustomFieldPO::getTemplateId, id)
                        .orderByAsc(TicketTemplateCustomFieldPO::getSortOrder));
        Map<Long, CustomFieldDefinitionPO> definitions = loadDefinitions(relations);

        List<TemplateFieldOutput> fields = new ArrayList<>();
        fields.add(systemField("title", "标题", "INPUT", "单行文本", 1));
        fields.add(systemField("description", "问题描述", "TEXTAREA", "多行文本", 2));
        for (TicketTemplateCustomFieldPO relation : relations) {
            CustomFieldDefinitionPO definition = definitions.get(relation.getCustomFieldId());
            if (definition != null) {
                fields.add(toFieldOutput(relation, definition));
            }
        }

        TemplateDetailOutput output = new TemplateDetailOutput();
        output.setId(template.getId());
        output.setTemplateCode(template.getTemplateCode());
        output.setName(template.getName());
        output.setDescription(template.getDescription());
        output.setIsSystem(template.getIsSystem());
        output.setIsActive(template.getIsActive());
        output.setFieldCount(fields.size());
        output.setCreateTime(template.getCreateTime());
        output.setUpdateTime(template.getUpdateTime());
        output.setFields(fields);
        return output;
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateDetailOutput createTemplate(TemplateCreateInput input) {
        String name = input.getName().trim();
        validateUniqueName(name, null);
        List<TemplateCustomFieldInput> fields = normalizeFields(input.getCustomFields());

        TicketTemplatePO template = new TicketTemplatePO();
        template.setTemplateCode(generateTemplateCode());
        template.setName(name);
        template.setDescription(trimToNull(input.getDescription()));
        template.setCategoryId(null);
        template.setFieldsConfig("[]");
        template.setIsSystem(0);
        template.setSortOrder(0);
        template.setIsActive(1);
        templateMapper.insert(template);
        saveFields(template, fields);
        return getDetail(template.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateDetailOutput updateTemplate(TemplateUpdateInput input) {
        TicketTemplatePO template = requireTemplate(input.getId());
        boolean systemTemplate = isSystemTemplate(template);
        if (input.getName() != null && !systemTemplate) {
            String name = input.getName().trim();
            validateUniqueName(name, template.getId());
            template.setName(name);
        }
        if (input.getDescription() != null) {
            template.setDescription(trimToNull(input.getDescription()));
        }
        if (input.getIsActive() != null) {
            int nextStatus = normalizeFlag(input.getIsActive());
            if (systemTemplate && nextStatus == 0) {
                throw BusinessException.of(ErrorCode.DATA_STATUS_ERROR, "默认模板不能停用");
            }
            template.setIsActive(nextStatus);
        }
        templateMapper.updateById(template);
        if (input.getCustomFields() != null) {
            saveFields(template, normalizeFields(input.getCustomFields()));
        }
        return getDetail(template.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        TicketTemplatePO template = requireTemplate(id);
        if (isSystemTemplate(template)) {
            throw BusinessException.of(ErrorCode.DATA_STATUS_ERROR, "默认模板不能删除");
        }
        Long categoryCount = categoryMapper.selectCount(new LambdaQueryWrapper<TicketCategoryPO>()
                .eq(TicketCategoryPO::getTemplateId, id));
        if (categoryCount != null && categoryCount > 0) {
            throw BusinessException.of(ErrorCode.DATA_STATUS_ERROR, "模板已被工单分类使用，请先解除关联");
        }
        Long ticketCount = ticketMapper.selectCount(new LambdaQueryWrapper<TicketPO>()
                .eq(TicketPO::getTemplateId, id));
        if (ticketCount != null && ticketCount > 0) {
            throw BusinessException.of(ErrorCode.DATA_STATUS_ERROR, "模板已有历史工单使用，不能删除");
        }
        templateFieldMapper.physicalDeleteByTemplateId(id);
        templateMapper.deleteById(id);
    }

    private void saveFields(TicketTemplatePO template, List<TemplateCustomFieldInput> inputs) {
        templateFieldMapper.physicalDeleteByTemplateId(template.getId());
        Map<Long, CustomFieldDefinitionPO> definitions = validateDefinitions(inputs);
        int order = 3;
        for (TemplateCustomFieldInput input : inputs) {
            TicketTemplateCustomFieldPO relation = new TicketTemplateCustomFieldPO();
            relation.setTemplateId(template.getId());
            relation.setCustomFieldId(input.getCustomFieldId());
            relation.setSortOrder(order++);
            relation.setIsEnabled(input.getIsEnabled() == null ? 1 : normalizeFlag(input.getIsEnabled()));
            relation.setIsRequired(input.getIsRequired() == null
                    ? definitions.get(input.getCustomFieldId()).getDefaultRequired()
                    : normalizeFlag(input.getIsRequired()));
            relation.setPlaceholderOverride(trimToNull(input.getPlaceholderOverride()));
            relation.setDefaultValueOverride(trimToNull(input.getDefaultValueOverride()));
            templateFieldMapper.insert(relation);
        }
        template.setFieldsConfig(buildLegacyFieldsConfig(inputs, definitions));
        templateMapper.updateById(template);
    }

    private List<TemplateCustomFieldInput> normalizeFields(List<TemplateCustomFieldInput> inputs) {
        if (inputs == null) {
            return Collections.emptyList();
        }
        List<TemplateCustomFieldInput> result = new ArrayList<>(inputs);
        result.sort(Comparator.comparing(item -> item.getSortOrder() == null ? Integer.MAX_VALUE : item.getSortOrder()));
        return result;
    }

    private Map<Long, CustomFieldDefinitionPO> validateDefinitions(List<TemplateCustomFieldInput> inputs) {
        Set<Long> ids = new HashSet<>();
        for (TemplateCustomFieldInput input : inputs) {
            if (!ids.add(input.getCustomFieldId())) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "同一个自定义字段不能重复添加");
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, CustomFieldDefinitionPO> definitions = customFieldMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CustomFieldDefinitionPO::getId, item -> item));
        if (definitions.size() != ids.size()) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "部分自定义字段不存在");
        }
        for (CustomFieldDefinitionPO definition : definitions.values()) {
            if (!Integer.valueOf(1).equals(definition.getIsActive())) {
                throw BusinessException.of(ErrorCode.DATA_STATUS_ERROR,
                        "自定义字段已停用，不能添加：" + definition.getFieldTitle());
            }
        }
        return definitions;
    }

    private Map<Long, CustomFieldDefinitionPO> loadDefinitions(List<TicketTemplateCustomFieldPO> relations) {
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = relations.stream().map(TicketTemplateCustomFieldPO::getCustomFieldId).collect(Collectors.toSet());
        return customFieldMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CustomFieldDefinitionPO::getId, item -> item));
    }

    private String buildLegacyFieldsConfig(List<TemplateCustomFieldInput> inputs,
                                           Map<Long, CustomFieldDefinitionPO> definitions) {
        List<Map<String, Object>> configs = new ArrayList<>();
        for (TemplateCustomFieldInput input : inputs) {
            if (Integer.valueOf(0).equals(input.getIsEnabled())) {
                continue;
            }
            CustomFieldDefinitionPO definition = definitions.get(input.getCustomFieldId());
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("key", definition.getFieldCode());
            config.put("label", definition.getFieldTitle());
            config.put("type", legacyType(definition.getFieldType()));
            config.put("placeholder", StringUtils.hasText(input.getPlaceholderOverride())
                    ? input.getPlaceholderOverride().trim() : definition.getPlaceholder());
            config.put("required", input.getIsRequired() == null
                    ? Integer.valueOf(1).equals(definition.getDefaultRequired())
                    : Integer.valueOf(1).equals(input.getIsRequired()));
            if (StringUtils.hasText(definition.getOptionConfig())) {
                config.put("options", JSON.parseArray(definition.getOptionConfig()));
            }
            configs.add(config);
        }
        return JSON.toJSONString(configs);
    }

    private TemplateFieldOutput systemField(String code, String title, String type, String typeLabel, int order) {
        TemplateFieldOutput output = new TemplateFieldOutput();
        output.setFieldSource("SYSTEM");
        output.setFieldCode(code);
        output.setFieldTitle(title);
        output.setFieldType(type);
        output.setFieldTypeLabel(typeLabel);
        output.setSortOrder(order);
        output.setIsEnabled(1);
        output.setIsRequired(1);
        output.setDeletable(false);
        output.setDefinitionActive(true);
        output.setOptions(Collections.emptyList());
        return output;
    }

    private TemplateFieldOutput toFieldOutput(TicketTemplateCustomFieldPO relation,
                                              CustomFieldDefinitionPO definition) {
        TemplateFieldOutput output = new TemplateFieldOutput();
        output.setFieldSource("CUSTOM");
        output.setCustomFieldId(definition.getId());
        output.setFieldCode(definition.getFieldCode());
        output.setFieldTitle(definition.getFieldTitle());
        output.setFieldType(definition.getFieldType());
        output.setFieldTypeLabel(typeLabel(definition.getFieldType()));
        output.setSortOrder(relation.getSortOrder());
        output.setIsEnabled(relation.getIsEnabled());
        output.setIsRequired(relation.getIsRequired());
        output.setPlaceholder(StringUtils.hasText(relation.getPlaceholderOverride())
                ? relation.getPlaceholderOverride() : definition.getPlaceholder());
        output.setDefaultValue(StringUtils.hasText(relation.getDefaultValueOverride())
                ? relation.getDefaultValueOverride() : definition.getDefaultValue());
        output.setOptions(StringUtils.hasText(definition.getOptionConfig())
                ? JSON.parseArray(definition.getOptionConfig(), TemplateFieldOptionOutput.class)
                : Collections.emptyList());
        output.setDeletable(true);
        output.setDefinitionActive(Integer.valueOf(1).equals(definition.getIsActive()));
        return output;
    }

    private TemplateAdminListOutput toAdminOutput(TicketTemplatePO template) {
        TemplateAdminListOutput output = new TemplateAdminListOutput();
        output.setId(template.getId());
        output.setTemplateCode(template.getTemplateCode());
        output.setName(template.getName());
        output.setDescription(template.getDescription());
        output.setIsSystem(template.getIsSystem());
        output.setIsActive(template.getIsActive());
        Long customCount = templateFieldMapper.selectCount(new LambdaQueryWrapper<TicketTemplateCustomFieldPO>()
                .eq(TicketTemplateCustomFieldPO::getTemplateId, template.getId()));
        output.setFieldCount(2 + (customCount == null ? 0 : customCount.intValue()));
        output.setCreateTime(template.getCreateTime());
        output.setUpdateTime(template.getUpdateTime());
        return output;
    }

    private TemplateListOutput toLegacyOutput(TicketTemplatePO template) {
        TemplateListOutput output = new TemplateListOutput();
        output.setId(template.getId());
        output.setName(template.getName());
        output.setCategoryId(template.getCategoryId());
        if (template.getCategoryId() != null) {
            TicketCategoryPO category = categoryMapper.selectById(template.getCategoryId());
            output.setCategoryName(category == null ? null : category.getName());
        }
        output.setFieldsConfig(template.getFieldsConfig());
        output.setDescription(template.getDescription());
        output.setIsActive(template.getIsActive());
        output.setCreateTime(template.getCreateTime());
        return output;
    }

    private TicketTemplatePO requireTemplate(Long id) {
        TicketTemplatePO template = templateMapper.selectById(id);
        if (template == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "模板不存在");
        }
        return template;
    }

    private void validateUniqueName(String name, Long excludedId) {
        LambdaQueryWrapper<TicketTemplatePO> query = new LambdaQueryWrapper<TicketTemplatePO>()
                .eq(TicketTemplatePO::getName, name);
        if (excludedId != null) {
            query.ne(TicketTemplatePO::getId, excludedId);
        }
        Long count = templateMapper.selectCount(query);
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.DATA_ALREADY_EXISTS, "模板名称已存在：" + name);
        }
    }

    private boolean isSystemTemplate(TicketTemplatePO template) {
        return Integer.valueOf(1).equals(template.getIsSystem())
                || SYSTEM_DEFAULT_CODE.equals(template.getTemplateCode());
    }

    private String generateTemplateCode() {
        return "tpl_" + UUID.randomUUID().toString().replace("-", "");
    }

    private int normalizeFlag(Integer value) {
        if (value == null || (value != 0 && value != 1)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "状态值只能是0或1");
        }
        return value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String legacyType(String type) {
        switch (type.toUpperCase(Locale.ROOT)) {
            case "TEXTAREA": return "textarea";
            case "SELECT":
            case "RADIO":
            case "CHECKBOX":
            case "CASCADER": return "select";
            case "DATE": return "date";
            default: return "input";
        }
    }

    private String typeLabel(String type) {
        switch (type) {
            case "INPUT": return "单行文本";
            case "TEXTAREA": return "多行文本";
            case "SELECT": return "下拉列表";
            case "ATTACHMENT": return "附件";
            case "DATE": return "日期";
            case "TIME": return "时间";
            case "DATETIME": return "日期和时间";
            case "NUMBER": return "数值";
            case "RADIO": return "单选框";
            case "CHECKBOX": return "复选框";
            case "CASCADER": return "级联";
            default: return type;
        }
    }
}
