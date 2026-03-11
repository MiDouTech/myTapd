package com.miduo.cloud.ticket.application.ticket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.BugChangeTypeEnum;
import com.miduo.cloud.ticket.entity.dto.ticket.BugChangeHistoryOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.BugFieldChangeItem;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 缺陷变更历史查询应用服务
 * 接口编号：API000501
 */
@Service
public class TicketChangeHistoryApplicationService extends BaseApplicationService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final TicketLogMapper ticketLogMapper;
    private final SysUserMapper sysUserMapper;

    public TicketChangeHistoryApplicationService(TicketLogMapper ticketLogMapper,
                                                  SysUserMapper sysUserMapper) {
        this.ticketLogMapper = ticketLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查询缺陷变更历史列表
     * 接口编号：API000501
     *
     * @param ticketId   工单 ID
     * @param changeType 变更类型筛选（BugChangeTypeEnum.code），null 表示全部
     * @param fieldName  变更字段筛选（英文字段名），null 表示全部（内存过滤）
     */
    public List<BugChangeHistoryOutput> listChangeHistory(Long ticketId, String changeType, String fieldName) {
        List<TicketLogPO> logs = ticketLogMapper.selectChangeHistoryByTicketId(ticketId, changeType);

        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyList();
        }

        Set<Long> userIds = logs.stream()
                .map(TicketLogPO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUserPO> userMap = batchQueryUsers(userIds);

        List<BugChangeHistoryOutput> result = new ArrayList<>();
        int seq = 1;
        for (TicketLogPO log : logs) {
            BugChangeHistoryOutput output = buildOutput(log, userMap, seq);
            if (output == null) {
                continue;
            }
            if (StringUtils.hasText(fieldName)) {
                List<BugFieldChangeItem> filtered = output.getFields().stream()
                        .filter(f -> fieldName.equals(f.getFieldName()))
                        .collect(Collectors.toList());
                if (filtered.isEmpty()) {
                    continue;
                }
                output.setFields(filtered);
            }
            result.add(output);
            seq++;
        }
        return result;
    }

    private Map<Long, SysUserPO> batchQueryUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(new ArrayList<>(userIds));
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(SysUserPO::getId, u -> u));
    }

    private BugChangeHistoryOutput buildOutput(TicketLogPO log, Map<Long, SysUserPO> userMap, int seq) {
        List<BugFieldChangeItem> fields = parseFieldsFromRemark(log.getRemark());
        if (fields == null) {
            return null;
        }

        BugChangeHistoryOutput output = new BugChangeHistoryOutput();
        output.setId(log.getId());
        output.setSeq(seq);
        output.setChangeTime(formatDate(log.getCreateTime()));

        SysUserPO user = log.getUserId() != null ? userMap.get(log.getUserId()) : null;
        output.setChangeByUserId(log.getUserId());
        output.setChangeByUserName(user != null ? user.getName() : "系统");
        output.setChangeByAvatar(user != null ? user.getAvatarUrl() : null);

        String changeTypeCode = extractChangeTypeFromRemark(log.getRemark());
        BugChangeTypeEnum type = BugChangeTypeEnum.fromCode(changeTypeCode);
        if (type != null) {
            output.setChangeType(type.getCode());
            output.setChangeTypeLabel(type.getLabel());
        } else {
            output.setChangeType(changeTypeCode);
            output.setChangeTypeLabel(changeTypeCode);
        }

        output.setFields(fields);
        return output;
    }

    /**
     * 解析 remark 字段中的 fields 数组
     * remark 格式：{"changeType":"MANUAL_CHANGE","fields":[...]}
     * 若 remark 非 JSON 格式（历史旧数据），返回 null（跳过此条记录）
     */
    private List<BugFieldChangeItem> parseFieldsFromRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        try {
            JSONObject jsonObj = JSON.parseObject(remark);
            if (jsonObj == null || !jsonObj.containsKey("fields")) {
                return null;
            }
            List<BugFieldChangeItem> fields = JSON.parseArray(
                    jsonObj.getString("fields"), BugFieldChangeItem.class);
            return fields != null ? fields : Collections.emptyList();
        } catch (Exception e) {
            log.debug("remark 非新格式 JSON，跳过此日志记录，remark={}", remark);
            return null;
        }
    }

    private String extractChangeTypeFromRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        try {
            JSONObject jsonObj = JSON.parseObject(remark);
            if (jsonObj == null) {
                return null;
            }
            return jsonObj.getString("changeType");
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }
}
