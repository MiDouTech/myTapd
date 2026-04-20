package com.miduo.cloud.ticket.application.ticket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.BugChangeTypeEnum;
import com.miduo.cloud.ticket.common.util.DisplayTimeFormat;
import com.miduo.cloud.ticket.common.enums.TicketAction;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.entity.dto.ticket.BugChangeHistoryOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.BugFieldChangeItem;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缺陷变更历史查询应用服务
 * 接口编号：API000501
 */
@Service
public class TicketChangeHistoryApplicationService extends BaseApplicationService {

    private final TicketLogMapper ticketLogMapper;
    private final SysUserMapper sysUserMapper;

    public TicketChangeHistoryApplicationService(TicketLogMapper ticketLogMapper,
                                                  SysUserMapper sysUserMapper) {
        this.ticketLogMapper = ticketLogMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查询缺陷变更历史列表（含字段级变更与生命周期事件）
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

        Set<Long> userIds = new HashSet<>();
        for (TicketLogPO l : logs) {
            if (l.getUserId() != null) {
                userIds.add(l.getUserId());
            }
            // ASSIGN 日志的 old_value / new_value 是被分派人的用户 ID，需一并批量查询
            if ("ASSIGN".equals(l.getAction())) {
                Long oldId = parseLongSilently(l.getOldValue());
                Long newId = parseLongSilently(l.getNewValue());
                if (oldId != null) userIds.add(oldId);
                if (newId != null) userIds.add(newId);
            }
        }
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
        boolean isLifecycleEvent = false;

        if (fields == null) {
            // remark 非 JSON 格式，尝试将其作为生命周期事件处理
            fields = buildLifecycleFields(log, userMap);
            if (fields == null) {
                return null;
            }
            isLifecycleEvent = true;
        }

        BugChangeHistoryOutput output = new BugChangeHistoryOutput();
        output.setId(log.getId());
        output.setSeq(seq);
        output.setChangeTime(formatDate(log.getCreateTime()));

        SysUserPO user = log.getUserId() != null ? userMap.get(log.getUserId()) : null;
        output.setChangeByUserId(log.getUserId());
        output.setChangeByUserName(user != null ? user.getName() : "系统");
        output.setChangeByAvatar(user != null ? user.getAvatarUrl() : null);

        if (isLifecycleEvent) {
            String changeTypeCode = mapActionToChangeType(log.getAction());
            output.setChangeType(changeTypeCode);
            TicketAction ticketAction = TicketAction.fromCode(log.getAction());
            if (ticketAction != null) {
                output.setChangeTypeLabel(ticketAction.getLabel());
            } else {
                BugChangeTypeEnum type = BugChangeTypeEnum.fromCode(changeTypeCode);
                output.setChangeTypeLabel(type != null ? type.getLabel() : log.getAction());
            }
        } else {
            String changeTypeCode = extractChangeTypeFromRemark(log.getRemark());
            BugChangeTypeEnum type = BugChangeTypeEnum.fromCode(changeTypeCode);
            if (type != null) {
                output.setChangeType(type.getCode());
                output.setChangeTypeLabel(type.getLabel());
            } else {
                output.setChangeType(changeTypeCode);
                output.setChangeTypeLabel(changeTypeCode);
            }
        }

        output.setFields(fields);
        return output;
    }

    /**
     * 为非 JSON 格式的生命周期日志构建展示字段列表
     * 返回 null 表示该日志没有有效展示内容，应被跳过
     */
    private List<BugFieldChangeItem> buildLifecycleFields(TicketLogPO log,
                                                           Map<Long, SysUserPO> userMap) {
        String action = log.getAction();
        if (action == null) {
            return null;
        }
        List<BugFieldChangeItem> fields = new ArrayList<>();
        switch (action) {
            case "CREATE": {
                if (StringUtils.hasText(log.getRemark())) {
                    BugFieldChangeItem item = new BugFieldChangeItem();
                    item.setFieldName("remark");
                    item.setFieldLabel("操作描述");
                    item.setNewValue(log.getRemark());
                    item.setNewLabel(log.getRemark());
                    fields.add(item);
                }
                return fields;
            }
            case "ASSIGN": {
                BugFieldChangeItem item = new BugFieldChangeItem();
                item.setFieldName("assignee");
                item.setFieldLabel("处理人");
                item.setOldValue(log.getOldValue());
                item.setOldLabel(resolveUserName(parseLongSilently(log.getOldValue()), userMap, log.getOldValue()));
                item.setNewValue(log.getNewValue());
                item.setNewLabel(resolveUserName(parseLongSilently(log.getNewValue()), userMap, log.getNewValue()));
                fields.add(item);
                if (StringUtils.hasText(log.getRemark())) {
                    BugFieldChangeItem remarkItem = new BugFieldChangeItem();
                    remarkItem.setFieldName("remark");
                    remarkItem.setFieldLabel("备注");
                    remarkItem.setNewValue(log.getRemark());
                    remarkItem.setNewLabel(log.getRemark());
                    fields.add(remarkItem);
                }
                return fields;
            }
            case "TRANSIT":
            case "RETURN": {
                if (StringUtils.hasText(log.getOldValue()) || StringUtils.hasText(log.getNewValue())) {
                    BugFieldChangeItem item = new BugFieldChangeItem();
                    item.setFieldName("status");
                    item.setFieldLabel("状态");
                    item.setOldValue(log.getOldValue());
                    item.setOldLabel(resolveStatusLabel(log.getOldValue()));
                    item.setNewValue(log.getNewValue());
                    item.setNewLabel(resolveStatusLabel(log.getNewValue()));
                    fields.add(item);
                }
                if (StringUtils.hasText(log.getRemark())) {
                    BugFieldChangeItem remarkItem = new BugFieldChangeItem();
                    remarkItem.setFieldName("remark");
                    remarkItem.setFieldLabel("备注");
                    remarkItem.setNewValue(log.getRemark());
                    remarkItem.setNewLabel(log.getRemark());
                    fields.add(remarkItem);
                }
                return fields;
            }
            case "TRANSFER": {
                if (StringUtils.hasText(log.getRemark())) {
                    BugFieldChangeItem item = new BugFieldChangeItem();
                    item.setFieldName("remark");
                    item.setFieldLabel("原因");
                    item.setNewValue(log.getRemark());
                    item.setNewLabel(log.getRemark());
                    fields.add(item);
                }
                return fields.isEmpty() ? null : fields;
            }
            case "FOLLOW":
            case "UNFOLLOW":
                // 关注/取消关注无需额外字段，返回空列表即可显示操作标签
                return fields;
            default: {
                if (StringUtils.hasText(log.getRemark())) {
                    BugFieldChangeItem item = new BugFieldChangeItem();
                    item.setFieldName("remark");
                    item.setFieldLabel("备注");
                    item.setNewValue(log.getRemark());
                    item.setNewLabel(log.getRemark());
                    fields.add(item);
                    return fields;
                }
                return null;
            }
        }
    }

    /**
     * 将 TicketAction 的 action 码映射到 BugChangeTypeEnum
     */
    private String mapActionToChangeType(String action) {
        if (action == null) {
            return BugChangeTypeEnum.SYSTEM_AUTO.getCode();
        }
        switch (action) {
            case "CREATE":
                return BugChangeTypeEnum.CREATE.getCode();
            case "TRANSIT":
            case "RETURN":
                return BugChangeTypeEnum.STATUS_CHANGE.getCode();
            default:
                return BugChangeTypeEnum.SYSTEM_AUTO.getCode();
        }
    }

    /**
     * 根据状态码获取状态名称（优先使用 TicketStatus 枚举）
     */
    private String resolveStatusLabel(String statusCode) {
        if (!StringUtils.hasText(statusCode)) {
            return statusCode;
        }
        TicketStatus status = TicketStatus.fromCode(statusCode);
        return status != null ? status.getLabel() : statusCode;
    }

    /**
     * 根据用户 ID 从 userMap 中获取姓名，找不到时降级展示 fallback
     */
    private String resolveUserName(Long userId, Map<Long, SysUserPO> userMap, String fallback) {
        if (userId == null) {
            return fallback;
        }
        SysUserPO user = userMap.get(userId);
        return user != null ? user.getName() : fallback;
    }

    /**
     * 安全地将字符串解析为 Long，解析失败返回 null
     */
    private Long parseLongSilently(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
        return DisplayTimeFormat.formatDateTime(date);
    }
}
