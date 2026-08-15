package com.miduo.cloud.ticket.application.customfield;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.customfield.CustomFieldCreateInput;
import com.miduo.cloud.ticket.entity.dto.customfield.CustomFieldOptionInput;
import com.miduo.cloud.ticket.entity.dto.customfield.CustomFieldOutput;
import com.miduo.cloud.ticket.entity.dto.customfield.CustomFieldUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.CustomFieldDefinitionMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.CustomFieldDefinitionPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomFieldApplicationService {

    private static final Set<String> SUPPORTED_TYPES = new HashSet<>(Arrays.asList(
            "INPUT", "TEXTAREA", "SELECT", "ATTACHMENT", "DATE", "TIME", "DATETIME",
            "NUMBER", "RADIO", "CHECKBOX", "CASCADER"));
    private static final Set<String> OPTION_TYPES = new HashSet<>(Arrays.asList(
            "SELECT", "RADIO", "CHECKBOX", "CASCADER"));
    private static final List<String> RESERVED_PREFIXES = Arrays.asList("sys_", "ticket_", "workflow_");

    private final CustomFieldDefinitionMapper customFieldMapper;

    public CustomFieldApplicationService(CustomFieldDefinitionMapper customFieldMapper) {
        this.customFieldMapper = customFieldMapper;
    }

    public List<CustomFieldOutput> listFields() {
        return customFieldMapper.selectList(new LambdaQueryWrapper<CustomFieldDefinitionPO>()
                        .orderByDesc(CustomFieldDefinitionPO::getCreateTime))
                .stream().map(this::toOutput).collect(Collectors.toList());
    }

    public CustomFieldOutput getField(Long id) {
        return toOutput(requireField(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomFieldOutput createField(CustomFieldCreateInput input) {
        String fieldCode = input.getFieldCode().trim();
        String fieldType = normalizeType(input.getFieldType());
        validateReservedPrefix(fieldCode);
        validateUniqueCode(fieldCode, null);
        validateOptions(fieldType, input.getOptions());

        CustomFieldDefinitionPO po = new CustomFieldDefinitionPO();
        po.setFieldCode(fieldCode);
        po.setFieldTitle(input.getFieldTitle().trim());
        po.setFieldType(fieldType);
        po.setDescription(trimToNull(input.getDescription()));
        po.setPlaceholder(trimToNull(input.getPlaceholder()));
        po.setDefaultRequired(normalizeFlag(input.getDefaultRequired(), 0));
        po.setDefaultValue(trimToNull(input.getDefaultValue()));
        po.setValidationConfig(toValidationJson(input.getMaxLength()));
        po.setOptionConfig(toOptionsJson(input.getOptions()));
        po.setIsActive(1);
        customFieldMapper.insert(po);
        return toOutput(po);
    }

    @Transactional(rollbackFor = Exception.class)
    public CustomFieldOutput updateField(CustomFieldUpdateInput input) {
        CustomFieldDefinitionPO po = requireField(input.getId());
        if (input.getFieldTitle() != null) {
            po.setFieldTitle(input.getFieldTitle().trim());
        }
        if (input.getDescription() != null) {
            po.setDescription(trimToNull(input.getDescription()));
        }
        if (input.getPlaceholder() != null) {
            po.setPlaceholder(trimToNull(input.getPlaceholder()));
        }
        if (input.getDefaultRequired() != null) {
            po.setDefaultRequired(normalizeFlag(input.getDefaultRequired(), 0));
        }
        if (input.getDefaultValue() != null) {
            po.setDefaultValue(trimToNull(input.getDefaultValue()));
        }
        if (input.getMaxLength() != null) {
            po.setValidationConfig(toValidationJson(input.getMaxLength()));
        }
        if (input.getOptions() != null) {
            validateOptions(po.getFieldType(), input.getOptions());
            po.setOptionConfig(toOptionsJson(input.getOptions()));
        }
        if (input.getIsActive() != null) {
            po.setIsActive(normalizeFlag(input.getIsActive(), po.getIsActive()));
        }
        customFieldMapper.updateById(po);
        return toOutput(requireField(po.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long id) {
        requireField(id);
        // 模板字段表落地后在此追加引用检查；当前字段池尚未接入模板，引用数恒为0。
        customFieldMapper.deleteById(id);
    }

    private CustomFieldDefinitionPO requireField(Long id) {
        CustomFieldDefinitionPO po = customFieldMapper.selectById(id);
        if (po == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "自定义字段不存在");
        }
        return po;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(normalized)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "不支持的字段类型：" + type);
        }
        return normalized;
    }

    private void validateReservedPrefix(String code) {
        for (String prefix : RESERVED_PREFIXES) {
            if (code.startsWith(prefix)) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "字段编码不能使用保留前缀：" + prefix);
            }
        }
    }

    private void validateUniqueCode(String fieldCode, Long excludedId) {
        LambdaQueryWrapper<CustomFieldDefinitionPO> query = new LambdaQueryWrapper<CustomFieldDefinitionPO>()
                .eq(CustomFieldDefinitionPO::getFieldCode, fieldCode);
        if (excludedId != null) {
            query.ne(CustomFieldDefinitionPO::getId, excludedId);
        }
        Long count = customFieldMapper.selectCount(query);
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.DATA_ALREADY_EXISTS, "字段编码已存在：" + fieldCode);
        }
    }

    private void validateOptions(String fieldType, List<CustomFieldOptionInput> options) {
        if (!OPTION_TYPES.contains(fieldType)) {
            return;
        }
        if (options == null || options.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "选择类型字段至少需要一个选项");
        }
        Set<String> values = new HashSet<>();
        for (CustomFieldOptionInput option : options) {
            String value = option.getValue().trim();
            if (!values.add(value)) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "选项值不能重复：" + value);
            }
        }
    }

    private Integer normalizeFlag(Integer value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value != 0 && value != 1) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "状态值只能是0或1");
        }
        return value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toValidationJson(Integer maxLength) {
        if (maxLength == null) {
            return null;
        }
        if (maxLength < 1 || maxLength > 10000) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "最大长度必须在1-10000之间");
        }
        return "{\"maxLength\":" + maxLength + "}";
    }

    private String toOptionsJson(List<CustomFieldOptionInput> options) {
        return options == null || options.isEmpty() ? null : JSON.toJSONString(options);
    }

    private CustomFieldOutput toOutput(CustomFieldDefinitionPO po) {
        CustomFieldOutput output = new CustomFieldOutput();
        output.setId(po.getId());
        output.setFieldCode(po.getFieldCode());
        output.setFieldTitle(po.getFieldTitle());
        output.setFieldType(po.getFieldType());
        output.setFieldTypeLabel(typeLabel(po.getFieldType()));
        output.setDescription(po.getDescription());
        output.setPlaceholder(po.getPlaceholder());
        output.setDefaultRequired(po.getDefaultRequired());
        output.setDefaultValue(po.getDefaultValue());
        output.setMaxLength(parseMaxLength(po.getValidationConfig()));
        output.setOptions(StringUtils.hasText(po.getOptionConfig())
                ? JSON.parseArray(po.getOptionConfig(), CustomFieldOptionInput.class) : Collections.emptyList());
        output.setIsActive(po.getIsActive());
        output.setReferenceCount(0);
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());
        return output;
    }

    private Integer parseMaxLength(String json) {
        return StringUtils.hasText(json) ? JSON.parseObject(json).getInteger("maxLength") : null;
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
