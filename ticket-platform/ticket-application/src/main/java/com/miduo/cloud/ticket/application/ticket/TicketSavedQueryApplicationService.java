package com.miduo.cloud.ticket.application.ticket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketSavedQueryOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketSavedQuerySaveInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketSavedQueryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketSavedQueryPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TicketSavedQueryApplicationService extends BaseApplicationService {
    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_SAVED_QUERIES = 50;
    private static final int MAX_CONFIG_LENGTH = 32768;
    private static final Set<String> QUERY_FIELDS = new HashSet<>(Arrays.asList(
            "ticketNo", "title", "companyName", "categoryId", "statuses", "excludeStatuses",
            "timeRange", "conditionLogic", "customConditions", "priority", "creatorId",
            "assigneeId", "slaStatus", "orderBy", "asc"
    ));
    private static final Set<String> EXPORT_FIELDS = new HashSet<>(Arrays.asList(
            "ticketNo", "companyName", "title", "categoryName", "priorityLabel", "statusLabel",
            "responseSlaStatusLabel", "resolveSlaStatusLabel", "creatorName", "assigneeName",
            "testAssigneeName", "urgeCount", "sourceLabel", "merchantNo", "severityLevel",
            "impactScope", "manualValidReportLabel", "expectedTime", "createTime", "updateTime",
            "resolvedAt", "closedAt"
    ));

    private final TicketSavedQueryMapper mapper;
    private final ObjectMapper objectMapper;

    public TicketSavedQueryApplicationService(TicketSavedQueryMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public List<TicketSavedQueryOutput> list(Long userId) {
        requireUser(userId);
        List<TicketSavedQueryOutput> result = new ArrayList<>();
        for (TicketSavedQueryPO po : mapper.selectByUserId(userId)) {
            result.add(toOutput(po));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public TicketSavedQueryOutput create(Long userId, TicketSavedQuerySaveInput input) {
        requireUser(userId);
        List<TicketSavedQueryPO> existing = mapper.selectByUserId(userId);
        if (existing.size() >= MAX_SAVED_QUERIES) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "每位用户最多保存50条常用查询");
        }
        String name = normalizeAndValidate(userId, null, input);
        TicketSavedQueryPO po = buildPo(userId, input, name);
        po.setSortOrder(existing.size() * 10);
        mapper.insert(po);
        return toOutput(mapper.selectOwnedById(po.getId(), userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public TicketSavedQueryOutput update(Long userId, Long id, TicketSavedQuerySaveInput input) {
        requireUser(userId);
        if (mapper.selectOwnedById(id, userId) == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "常用查询不存在");
        }
        String name = normalizeAndValidate(userId, id, input);
        TicketSavedQueryPO po = buildPo(userId, input, name);
        po.setId(id);
        if (mapper.updateOwned(po) == 0) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "常用查询不存在");
        }
        return toOutput(mapper.selectOwnedById(id, userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        requireUser(userId);
        if (mapper.softDeleteOwned(id, userId, SecurityUtil.getCurrentUsername()) == 0) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "常用查询不存在");
        }
    }

    private String normalizeAndValidate(Long userId, Long excludeId, TicketSavedQuerySaveInput input) {
        String name = input.getName().trim();
        if (name.length() > 30) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "查询名称不能超过30个字符");
        }
        if (!QUERY_FIELDS.containsAll(input.getQueryConfig().keySet())) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "查询配置包含不支持的字段");
        }
        LinkedHashSet<String> fields = new LinkedHashSet<>(input.getExportFields());
        if (fields.isEmpty() || !EXPORT_FIELDS.containsAll(fields)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "导出字段不合法");
        }
        input.setExportFields(new ArrayList<>(fields));
        if (mapper.countActiveByName(userId, name, excludeId) > 0) {
            throw BusinessException.of(ErrorCode.DATA_ALREADY_EXISTS, "常用查询名称已存在");
        }
        return name;
    }

    private TicketSavedQueryPO buildPo(Long userId, TicketSavedQuerySaveInput input, String name) {
        TicketSavedQueryPO po = new TicketSavedQueryPO();
        po.setUserId(userId);
        po.setQueryName(name);
        po.setQueryConfig(writeJson(input.getQueryConfig()));
        po.setExportFields(writeJson(input.getExportFields()));
        po.setSchemaVersion(SCHEMA_VERSION);
        po.setCreateBy(SecurityUtil.getCurrentUsername());
        po.setUpdateBy(SecurityUtil.getCurrentUsername());
        return po;
    }

    private TicketSavedQueryOutput toOutput(TicketSavedQueryPO po) {
        if (po == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "常用查询不存在");
        }
        try {
            TicketSavedQueryOutput output = new TicketSavedQueryOutput();
            output.setId(po.getId());
            output.setName(po.getQueryName());
            output.setQueryConfig(objectMapper.readValue(po.getQueryConfig(), new TypeReference<Map<String, Object>>() {}));
            output.setExportFields(objectMapper.readValue(po.getExportFields(), new TypeReference<List<String>>() {}));
            output.setSchemaVersion(po.getSchemaVersion());
            output.setSortOrder(po.getSortOrder());
            output.setCreateTime(po.getCreateTime());
            output.setUpdateTime(po.getUpdateTime());
            return output;
        } catch (JsonProcessingException e) {
            log.error("常用查询配置反序列化失败: id={}", po.getId(), e);
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "常用查询配置损坏");
        }
    }

    private String writeJson(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json.length() > MAX_CONFIG_LENGTH) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "常用查询配置过大");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "常用查询配置格式错误");
        }
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
    }
}
