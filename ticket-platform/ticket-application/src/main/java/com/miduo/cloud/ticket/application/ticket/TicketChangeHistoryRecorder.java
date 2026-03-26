package com.miduo.cloud.ticket.application.ticket;

import com.alibaba.fastjson2.JSON;
import com.miduo.cloud.ticket.common.enums.BugChangeTypeEnum;
import com.miduo.cloud.ticket.common.enums.SeverityLevel;
import com.miduo.cloud.ticket.common.enums.TicketAction;
import com.miduo.cloud.ticket.entity.dto.ticket.BugFieldChangeItem;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketBugDevInfoPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketBugInfoPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketBugTestInfoPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketBugCustomerInfoInput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketBugDevInfoInput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketBugTestInfoInput;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 缺陷变更历史记录工具
 * 职责：
 * 1. record() - 将字段变更列表序列化为 JSON 写入 ticket_log.remark
 * 2. detectXxxChanges() - 对比 PO 旧值和 Input 新值，生成 BugFieldChangeItem 列表
 *
 * 约束：text 类字段截断为前 200 字符，remark 列已扩展为 TEXT 类型无硬上限
 */
@Component
public class TicketChangeHistoryRecorder {

    private static final int TEXT_TRUNCATE_LENGTH = 200;

    private final TicketLogMapper ticketLogMapper;
    private final TicketTimeTrackApplicationService ticketTimeTrackApplicationService;

    public TicketChangeHistoryRecorder(TicketLogMapper ticketLogMapper,
                                       @Lazy TicketTimeTrackApplicationService ticketTimeTrackApplicationService) {
        this.ticketLogMapper = ticketLogMapper;
        this.ticketTimeTrackApplicationService = ticketTimeTrackApplicationService;
    }

    /**
     * 记录一批字段变更到 ticket_log
     * 若 changes 为空则不写入
     */
    public void record(Long ticketId, Long userId, BugChangeTypeEnum changeType,
                       List<BugFieldChangeItem> changes) {
        if (CollectionUtils.isEmpty(changes)) {
            return;
        }
        String remark = buildRemark(changeType, changes);
        TicketLogPO log = new TicketLogPO();
        log.setTicketId(ticketId);
        log.setUserId(userId != null ? userId : 0L);
        log.setAction(TicketAction.UPDATE.getCode());
        log.setRemark(remark);
        ticketLogMapper.insert(log);
    }

    /**
     * 写入 ticket_log 并同步一条时间链轨迹，便于「时间链」与字段变更 JSON 在时间窗内自动关联
     */
    public void recordWithTimeTrack(Long ticketId, Long userId, BugChangeTypeEnum changeType,
                                    List<BugFieldChangeItem> changes) {
        if (CollectionUtils.isEmpty(changes)) {
            return;
        }
        String remark = buildRemark(changeType, changes);
        TicketLogPO log = new TicketLogPO();
        log.setTicketId(ticketId);
        log.setUserId(userId != null ? userId : 0L);
        log.setAction(TicketAction.UPDATE.getCode());
        log.setRemark(remark);
        ticketLogMapper.insert(log);
        ticketTimeTrackApplicationService.recordFieldEditTrack(ticketId, userId, remark);
    }

    /**
     * 记录工单创建时的初始字段快照
     */
    public void recordCreate(Long ticketId, Long userId, List<BugFieldChangeItem> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        String remark = buildRemark(BugChangeTypeEnum.CREATE, fields);
        TicketLogPO log = new TicketLogPO();
        log.setTicketId(ticketId);
        log.setUserId(userId != null ? userId : 0L);
        log.setAction(TicketAction.CREATE.getCode());
        log.setRemark(remark);
        ticketLogMapper.insert(log);
    }

    /**
     * 检测客服信息变更（对比 PO 旧值和 Input 新值）
     * 变更检测必须在 apply 赋值之前调用
     */
    public List<BugFieldChangeItem> detectCustomerInfoChanges(TicketBugInfoPO old,
                                                               TicketBugCustomerInfoInput input) {
        List<BugFieldChangeItem> changes = new ArrayList<>();
        detectChange(changes, "company_name", "公司名称", old.getCompanyName(), input.getCompanyName());
        detectChange(changes, "merchant_no", "商户编号", old.getMerchantNo(), input.getMerchantNo());
        detectChange(changes, "merchant_account", "商户账号", old.getMerchantAccount(), input.getMerchantAccount());
        detectChange(changes, "problem_desc", "问题描述",
                truncate(old.getProblemDesc()), truncate(input.getProblemDesc()));
        detectChange(changes, "expected_result", "预期结果",
                truncate(old.getExpectedResult()), truncate(input.getExpectedResult()));
        detectChange(changes, "scene_code", "场景码", old.getSceneCode(), input.getSceneCode());
        detectChange(changes, "problem_screenshot", "问题截图",
                old.getProblemScreenshot(), input.getProblemScreenshot());
        return changes;
    }

    /**
     * 检测测试信息变更（对比 PO 旧值和 Input 新值）
     */
    public List<BugFieldChangeItem> detectTestInfoChanges(TicketBugTestInfoPO old,
                                                           TicketBugTestInfoInput input) {
        List<BugFieldChangeItem> changes = new ArrayList<>();
        String oldSeverity = normalizeSeverityCode(old.getSeverityLevel());
        String newSeverity = normalizeSeverityCode(input.getSeverityLevel());
        detectEnumChange(changes, "severity_level", "缺陷等级",
                oldSeverity, newSeverity, this::getSeverityLevelLabel);
        detectChange(changes, "reproduce_env", "复现环境", old.getReproduceEnv(), input.getReproduceEnv());
        detectChange(changes, "impact_scope", "影响范围", old.getImpactScope(), input.getImpactScope());
        detectChange(changes, "module_name", "所属模块", old.getModuleName(), input.getModuleName());
        detectChange(changes, "reproduce_steps", "复现步骤",
                truncate(old.getReproduceSteps()), truncate(input.getReproduceSteps()));
        detectChange(changes, "actual_result", "实际结果",
                truncate(old.getActualResult()), truncate(input.getActualResult()));
        detectChange(changes, "reproduce_screenshot", "复现截图",
                old.getReproduceScreenshot(), input.getReproduceScreenshot());
        detectChange(changes, "test_remark", "测试备注",
                truncate(old.getTestRemark()), truncate(input.getTestRemark()));
        return changes;
    }

    /**
     * 检测开发信息变更（对比 PO 旧值和 Input 新值）
     */
    public List<BugFieldChangeItem> detectDevInfoChanges(TicketBugDevInfoPO old,
                                                          TicketBugDevInfoInput input) {
        List<BugFieldChangeItem> changes = new ArrayList<>();
        detectChange(changes, "root_cause", "根因分析",
                truncate(old.getRootCause()), truncate(input.getRootCause()));
        detectChange(changes, "fix_solution", "修复方案",
                truncate(old.getFixSolution()), truncate(input.getFixSolution()));
        detectChange(changes, "git_branch", "Git分支", old.getGitBranch(), input.getGitBranch());
        detectChange(changes, "impact_assessment", "影响评估",
                truncate(old.getImpactAssessment()), truncate(input.getImpactAssessment()));
        return changes;
    }

    /**
     * 序列化变更批次为 JSON 字符串
     * 每个字段文本值已通过 truncate() 截断为前 200 字符，整体 JSON 大小受控
     */
    private String buildRemark(BugChangeTypeEnum changeType, List<BugFieldChangeItem> changes) {
        Map<String, Object> remarkMap = new HashMap<>();
        remarkMap.put("changeType", changeType.getCode());
        remarkMap.put("fields", changes);
        return JSON.toJSONString(remarkMap);
    }

    /**
     * 比较两个普通字符串值，不同则加入变更列表
     */
    private void detectChange(List<BugFieldChangeItem> list, String fieldName, String fieldLabel,
                               String oldVal, String newVal) {
        String normalizedOld = trimNull(oldVal);
        String normalizedNew = trimNull(newVal);
        if (!Objects.equals(normalizedOld, normalizedNew)) {
            BugFieldChangeItem item = new BugFieldChangeItem();
            item.setFieldName(fieldName);
            item.setFieldLabel(fieldLabel);
            item.setOldValue(emptyToNull(oldVal));
            item.setOldLabel(emptyToNull(oldVal));
            item.setNewValue(emptyToNull(newVal));
            item.setNewLabel(emptyToNull(newVal));
            list.add(item);
        }
    }

    /**
     * 比较枚举字段，旧值和新值均转换为 label 展示
     */
    private void detectEnumChange(List<BugFieldChangeItem> list, String fieldName, String fieldLabel,
                                   String oldCode, String newCode,
                                   java.util.function.Function<String, String> labelFn) {
        String normalizedOld = trimNull(oldCode);
        String normalizedNew = trimNull(newCode);
        if (!Objects.equals(normalizedOld, normalizedNew)) {
            BugFieldChangeItem item = new BugFieldChangeItem();
            item.setFieldName(fieldName);
            item.setFieldLabel(fieldLabel);
            item.setOldValue(emptyToNull(oldCode));
            item.setOldLabel(oldCode != null && !oldCode.isEmpty() ? labelFn.apply(oldCode) : null);
            item.setNewValue(emptyToNull(newCode));
            item.setNewLabel(newCode != null && !newCode.isEmpty() ? labelFn.apply(newCode) : null);
            list.add(item);
        }
    }

    private String getSeverityLevelLabel(String code) {
        SeverityLevel level = SeverityLevel.fromCode(normalizeSeverityCode(code));
        return level != null ? level.getLabel() : code;
    }

    private String normalizeSeverityCode(String source) {
        if (source == null) {
            return null;
        }
        String value = source.trim().toUpperCase(Locale.ROOT);
        if ("FATAL".equals(value)) {
            return "P0";
        }
        if ("CRITICAL".equals(value)) {
            return "P1";
        }
        if ("NORMAL".equals(value)) {
            return "P2";
        }
        if ("MINOR".equals(value)) {
            return "P3";
        }
        return value;
    }

    /**
     * 截断较长文本，防止 remark 字段超长
     */
    private String truncate(String val) {
        if (val == null) {
            return null;
        }
        return val.length() > TEXT_TRUNCATE_LENGTH ? val.substring(0, TEXT_TRUNCATE_LENGTH) + "..." : val;
    }

    private String trimNull(String val) {
        return val == null ? "" : val.trim();
    }

    private String emptyToNull(String val) {
        return (val == null || val.trim().isEmpty()) ? null : val;
    }
}
