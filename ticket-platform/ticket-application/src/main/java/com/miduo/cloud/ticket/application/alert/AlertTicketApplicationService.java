package com.miduo.cloud.ticket.application.alert;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.ticket.TicketApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketAssigneeSyncService;
import com.miduo.cloud.ticket.common.constants.AlertConstants;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.*;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.alert.*;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketCreateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.mapper.AlertEventLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.mapper.AlertRuleMappingMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.po.AlertEventLogPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.po.AlertRuleMappingPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCommentMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCommentPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警接入工单系统应用服务
 */
@Service
public class AlertTicketApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AlertTicketApplicationService.class);

    private static final long SYSTEM_USER_ID = 1L;

    @Resource
    private AlertRuleMappingMapper alertRuleMappingMapper;

    @Resource
    private AlertEventLogMapper alertEventLogMapper;

    @Resource
    private TicketApplicationService ticketApplicationService;

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private TicketCommentMapper commentMapper;

    @Resource
    private TicketCategoryMapper categoryMapper;

    @Resource
    private SysUserMapper userMapper;

    @Resource
    private TicketAssigneeSyncService ticketAssigneeSyncService;

    @Resource
    private SystemConfigMapper systemConfigMapper;

    /**
     * 处理夜莺告警事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void processAlertEvent(NightingaleAlertEvent event) {
        if (event == null) {
            log.warn("收到空告警事件，跳过");
            return;
        }

        String rawPayload = JSON.toJSONString(event);
        log.info("收到夜莺告警事件: ruleId={}, ruleName={}, severity={}, target={}, recovered={}",
                event.getRuleId(), event.getRuleName(), event.getSeverity(),
                event.getTargetIdent(), event.getIsRecovered());

        if (Boolean.TRUE.equals(event.getIsRecovered())) {
            handleRecoveredEvent(event, rawPayload);
            return;
        }

        handleAlertEvent(event, rawPayload);
    }

    private void handleRecoveredEvent(NightingaleAlertEvent event, String rawPayload) {
        LambdaQueryWrapper<AlertEventLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertEventLogPO::getEventHash, safeString(event.getHash()))
                .eq(AlertEventLogPO::getProcessResult, AlertProcessResult.CREATED.getCode())
                .isNotNull(AlertEventLogPO::getTicketId)
                .orderByDesc(AlertEventLogPO::getCreateTime)
                .last("LIMIT 1");
        AlertEventLogPO lastCreated = alertEventLogMapper.selectOne(wrapper);

        AlertEventLogPO logEntry = buildLogEntry(event, rawPayload);
        logEntry.setIsRecovered(true);
        logEntry.setProcessResult(AlertProcessResult.RECOVERED.getCode());

        if (lastCreated != null && lastCreated.getTicketId() != null) {
            logEntry.setTicketId(lastCreated.getTicketId());

            TicketPO ticket = ticketMapper.selectById(lastCreated.getTicketId());
            if (ticket != null && !isTerminalStatus(ticket.getStatus())) {
                addRecoveryComment(ticket.getId(), event);
            }
        }

        alertEventLogMapper.insert(logEntry);
        log.info("告警恢复事件已处理: hash={}, ticketId={}", event.getHash(), logEntry.getTicketId());
    }

    private void handleAlertEvent(NightingaleAlertEvent event, String rawPayload) {
        AlertRuleMappingPO mapping = findMapping(event.getRuleName());

        int dedupMinutes = (mapping != null) ? mapping.getDedupWindowMinutes() : 30;
        if (isDuplicate(event.getHash(), dedupMinutes)) {
            AlertEventLogPO logEntry = buildLogEntry(event, rawPayload);
            logEntry.setProcessResult(AlertProcessResult.DEDUP.getCode());
            alertEventLogMapper.insert(logEntry);
            log.info("告警事件去重跳过: hash={}, dedupWindow={}min", event.getHash(), dedupMinutes);
            return;
        }

        Long categoryId;
        String priority;

        if (mapping != null) {
            categoryId = mapping.getCategoryId();
            priority = mapPriority(event.getSeverity(), mapping);
        } else {
            categoryId = getDefaultCategoryId();
            priority = AlertSeverity.fromCode(event.getSeverity()).getDefaultPriority();
        }

        if (categoryId == null) {
            AlertEventLogPO logEntry = buildLogEntry(event, rawPayload);
            logEntry.setProcessResult(AlertProcessResult.UNMAPPED.getCode());
            alertEventLogMapper.insert(logEntry);
            log.warn("告警事件无可用工单分类，跳过创建: ruleName={}", event.getRuleName());
            return;
        }

        List<Long> assigneeIds = resolveAssigneeIds(event, mapping);
        Long firstAssigneeId = assigneeIds.isEmpty() ? null : assigneeIds.get(0);

        TicketCreateInput ticketInput = buildTicketCreateInput(event, categoryId, priority, firstAssigneeId);
        Long ticketId = ticketApplicationService.createTicket(ticketInput, SYSTEM_USER_ID);

        if (assigneeIds.size() > 1) {
            try {
                ticketAssigneeSyncService.replaceAndPersist(ticketId, assigneeIds);
                log.info("告警工单多人指派: ticketId={}, assignees={}", ticketId, assigneeIds);
            } catch (Exception e) {
                log.warn("告警工单多人指派失败，已保留首位处理人: ticketId={}, error={}",
                        ticketId, e.getMessage());
            }
        }

        AlertEventLogPO logEntry = buildLogEntry(event, rawPayload);
        logEntry.setTicketId(ticketId);
        logEntry.setProcessResult(AlertProcessResult.CREATED.getCode());
        alertEventLogMapper.insert(logEntry);

        log.info("告警事件已创建工单: hash={}, ticketId={}, assignees={}, mapping={}",
                event.getHash(), ticketId, assigneeIds,
                mapping != null ? "matched" : "default");
    }

    /**
     * 从夜莺推送的 notify_users_obj 中解析通知用户，
     * 依次按 phone、email、nickname(name) 匹配工单系统用户。
     * 匹配不到时回退到映射配置中的 assigneeId。
     */
    private List<Long> resolveAssigneeIds(NightingaleAlertEvent event, AlertRuleMappingPO mapping) {
        List<Long> matched = matchNotifyUsers(event.getNotifyUsersObj());

        if (!matched.isEmpty()) {
            return matched;
        }

        if (mapping != null && mapping.getAssigneeId() != null) {
            return Collections.singletonList(mapping.getAssigneeId());
        }

        return Collections.emptyList();
    }

    /**
     * 将夜莺 notify_users_obj 中的用户与工单系统 sys_user 进行匹配。
     * 匹配优先级：phone > email > nickname==name
     * 一次性批量加载所有活跃用户到内存 Map 进行匹配，避免循环查库。
     */
    private List<Long> matchNotifyUsers(List<NightingaleNotifyUser> notifyUsers) {
        if (notifyUsers == null || notifyUsers.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysUserPO> allUserQuery = new LambdaQueryWrapper<>();
        allUserQuery.eq(SysUserPO::getAccountStatus, 1);
        List<SysUserPO> allActiveUsers = userMapper.selectList(allUserQuery);
        if (allActiveUsers == null || allActiveUsers.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Long> phoneMap = new HashMap<>();
        Map<String, Long> emailMap = new HashMap<>();
        Map<String, Long> nameMap = new HashMap<>();
        for (SysUserPO u : allActiveUsers) {
            if (StringUtils.hasText(u.getPhone())) {
                phoneMap.put(u.getPhone().trim(), u.getId());
            }
            if (StringUtils.hasText(u.getEmail())) {
                emailMap.put(u.getEmail().trim().toLowerCase(), u.getId());
            }
            if (StringUtils.hasText(u.getName())) {
                nameMap.put(u.getName().trim(), u.getId());
            }
        }

        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (NightingaleNotifyUser nu : notifyUsers) {
            Long uid = null;
            if (uid == null && StringUtils.hasText(nu.getPhone())) {
                uid = phoneMap.get(nu.getPhone().trim());
            }
            if (uid == null && StringUtils.hasText(nu.getEmail())) {
                uid = emailMap.get(nu.getEmail().trim().toLowerCase());
            }
            if (uid == null && StringUtils.hasText(nu.getNickname())) {
                uid = nameMap.get(nu.getNickname().trim());
            }
            if (uid == null && StringUtils.hasText(nu.getUsername())) {
                uid = nameMap.get(nu.getUsername().trim());
            }
            if (uid != null) {
                result.add(uid);
            } else {
                log.debug("夜莺通知用户无法匹配工单系统: nickname={}, phone={}, email={}",
                        nu.getNickname(), nu.getPhone(), nu.getEmail());
            }
        }

        return new ArrayList<>(result);
    }

    private AlertRuleMappingPO findMapping(String ruleName) {
        if (!StringUtils.hasText(ruleName)) {
            return null;
        }

        LambdaQueryWrapper<AlertRuleMappingPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertRuleMappingPO::getEnabled, true);
        List<AlertRuleMappingPO> allMappings = alertRuleMappingMapper.selectList(wrapper);

        for (AlertRuleMappingPO mapping : allMappings) {
            AlertMatchMode mode = AlertMatchMode.fromCode(mapping.getMatchMode());
            if (mode == AlertMatchMode.EXACT && ruleName.equals(mapping.getRuleName())) {
                return mapping;
            }
            if (mode == AlertMatchMode.PREFIX && ruleName.startsWith(mapping.getRuleName())) {
                return mapping;
            }
        }
        return null;
    }

    private boolean isDuplicate(String hash, int dedupMinutes) {
        if (!StringUtils.hasText(hash) || dedupMinutes <= 0) {
            return false;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -dedupMinutes);
        Date windowStart = cal.getTime();

        LambdaQueryWrapper<AlertEventLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertEventLogPO::getEventHash, hash)
                .eq(AlertEventLogPO::getProcessResult, AlertProcessResult.CREATED.getCode())
                .ge(AlertEventLogPO::getCreateTime, windowStart)
                .last("LIMIT 1");

        return alertEventLogMapper.selectCount(wrapper) > 0;
    }

    private String mapPriority(Integer severity, AlertRuleMappingPO mapping) {
        AlertSeverity alertSeverity = AlertSeverity.fromCode(severity);
        switch (alertSeverity) {
            case P1:
                return StringUtils.hasText(mapping.getPriorityP1())
                        ? mapping.getPriorityP1() : Priority.URGENT.getCode();
            case P2:
                return StringUtils.hasText(mapping.getPriorityP2())
                        ? mapping.getPriorityP2() : Priority.HIGH.getCode();
            case P3:
            default:
                return StringUtils.hasText(mapping.getPriorityP3())
                        ? mapping.getPriorityP3() : Priority.MEDIUM.getCode();
        }
    }

    private TicketCreateInput buildTicketCreateInput(NightingaleAlertEvent event,
                                                     Long categoryId, String priority, Long assigneeId) {
        TicketCreateInput input = new TicketCreateInput();
        input.setTitle(buildTicketTitle(event));
        input.setDescription(buildTicketDescription(event));
        input.setCategoryId(categoryId);
        input.setPriority(priority);
        input.setAssigneeId(assigneeId);
        input.setSource(TicketSource.ALERT.getCode());
        return input;
    }

    private String buildTicketTitle(NightingaleAlertEvent event) {
        String severityLabel = AlertSeverity.fromCode(event.getSeverity()).getLabel();
        return String.format("[%s告警] %s - %s",
                severityLabel,
                safeString(event.getRuleName()),
                safeString(event.getTargetIdent()));
    }

    private String buildTicketDescription(NightingaleAlertEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 监控告警信息\n\n");

        sb.append("| 项目 | 详情 |\n");
        sb.append("|------|------|\n");
        sb.append(String.format("| **告警级别** | %s |\n", AlertSeverity.fromCode(event.getSeverity()).getLabel()));
        sb.append(String.format("| **规则名称** | %s |\n", safeString(event.getRuleName())));

        if (StringUtils.hasText(event.getRuleNote())) {
            sb.append(String.format("| **规则备注** | %s |\n", event.getRuleNote()));
        }

        sb.append(String.format("| **监控对象** | %s |\n", safeString(event.getTargetIdent())));

        if (StringUtils.hasText(event.getTargetNote())) {
            sb.append(String.format("| **对象备注** | %s |\n", event.getTargetNote()));
        }

        sb.append(String.format("| **触发值** | %s |\n", safeString(event.getTriggerValue())));

        if (event.getTriggerTime() != null) {
            sb.append(String.format("| **告警时间** | %s |\n", formatTimestamp(event.getTriggerTime())));
        }
        if (event.getFirstTriggerTime() != null) {
            sb.append(String.format("| **首次触发时间** | %s |\n", formatTimestamp(event.getFirstTriggerTime())));
        }

        if (StringUtils.hasText(event.getPromQl())) {
            sb.append(String.format("| **PromQL** | `%s` |\n", event.getPromQl()));
        }

        if (StringUtils.hasText(event.getRuleAlgo())) {
            sb.append(String.format("| **告警算法** | %s |\n", event.getRuleAlgo()));
        }

        if (StringUtils.hasText(event.getCate())) {
            sb.append(String.format("| **数据源类型** | %s |\n", event.getCate()));
        }

        if (event.getDatasourceId() != null) {
            sb.append(String.format("| **数据源ID** | %d |\n", event.getDatasourceId()));
        }

        if (event.getRuleId() != null) {
            sb.append(String.format("| **规则ID** | %d |\n", event.getRuleId()));
        }

        if (StringUtils.hasText(event.getClaimant())) {
            sb.append(String.format("| **认领人** | %s |\n", event.getClaimant()));
        }

        if (event.getAnnotations() != null && !event.getAnnotations().isEmpty()) {
            sb.append("\n### 注解信息\n\n");
            for (Map.Entry<String, String> entry : event.getAnnotations().entrySet()) {
                sb.append(String.format("- **%s**: %s\n", entry.getKey(), entry.getValue()));
            }
        }

        if (event.getTags() != null && !event.getTags().isEmpty()) {
            sb.append("\n### 监控标签\n\n");
            for (String tag : event.getTags()) {
                sb.append("- ").append(tag).append("\n");
            }
        }

        if (StringUtils.hasText(event.getGroupName())) {
            sb.append(String.format("\n**业务组**: %s\n", event.getGroupName()));
        }

        if (StringUtils.hasText(event.getRunbookUrl())) {
            sb.append(String.format("\n**运维手册**: %s\n", event.getRunbookUrl()));
        }

        return sb.toString();
    }

    private void addRecoveryComment(Long ticketId, NightingaleAlertEvent event) {
        TicketCommentPO comment = new TicketCommentPO();
        comment.setTicketId(ticketId);
        comment.setUserId(SYSTEM_USER_ID);
        comment.setContent(String.format(
                "**[监控告警恢复]** 规则「%s」的告警已恢复\n- 监控对象: %s\n- 恢复时间: %s",
                safeString(event.getRuleName()),
                safeString(event.getTargetIdent()),
                event.getTriggerTime() != null ? formatTimestamp(event.getTriggerTime()) : "未知"));
        comment.setType("system");
        commentMapper.insert(comment);
    }

    private AlertEventLogPO buildLogEntry(NightingaleAlertEvent event, String rawPayload) {
        AlertEventLogPO logEntry = new AlertEventLogPO();
        logEntry.setEventHash(safeString(event.getHash()));
        logEntry.setRuleId(event.getRuleId() != null ? event.getRuleId() : 0L);
        logEntry.setRuleName(safeString(event.getRuleName()));
        logEntry.setSeverity(event.getSeverity() != null ? event.getSeverity() : 3);
        logEntry.setTargetIdent(safeString(event.getTargetIdent()));
        logEntry.setTriggerValue(safeString(event.getTriggerValue()));
        logEntry.setTriggerTime(event.getTriggerTime() != null
                ? new Date(event.getTriggerTime() * 1000) : null);
        logEntry.setIsRecovered(Boolean.TRUE.equals(event.getIsRecovered()));
        logEntry.setRawPayload(rawPayload);
        return logEntry;
    }

    private Long getDefaultCategoryId() {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigKey, AlertConstants.CONFIG_KEY_DEFAULT_CATEGORY_ID);
        SystemConfigPO config = systemConfigMapper.selectOne(wrapper);
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            try {
                return Long.parseLong(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("告警默认分类ID配置无效: {}", config.getConfigValue());
            }
        }

        LambdaQueryWrapper<TicketCategoryPO> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.last("LIMIT 1");
        TicketCategoryPO firstCategory = categoryMapper.selectOne(catWrapper);
        return firstCategory != null ? firstCategory.getId() : null;
    }

    private boolean isTerminalStatus(String status) {
        return "closed".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status);
    }

    /**
     * 验证告警接入 Token
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigKey, AlertConstants.CONFIG_KEY_TOKEN);
        SystemConfigPO config = systemConfigMapper.selectOne(wrapper);
        return config != null && token.equals(config.getConfigValue());
    }

    /**
     * 获取当前告警接入 Token
     */
    public AlertTokenOutput getToken(String baseUrl) {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigKey, AlertConstants.CONFIG_KEY_TOKEN);
        SystemConfigPO config = systemConfigMapper.selectOne(wrapper);

        AlertTokenOutput output = new AlertTokenOutput();
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            output.setToken(config.getConfigValue());
            output.setWebhookUrl(baseUrl + "/api/open/alert/nightingale/webhook?token=" + config.getConfigValue());
        } else {
            output.setToken("");
            output.setWebhookUrl("");
        }
        return output;
    }

    /**
     * 重置告警接入 Token
     */
    @Transactional(rollbackFor = Exception.class)
    public AlertTokenOutput resetToken(String baseUrl) {
        String newToken = UUID.randomUUID().toString().replace("-", "");

        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigKey, AlertConstants.CONFIG_KEY_TOKEN);
        SystemConfigPO config = systemConfigMapper.selectOne(wrapper);

        if (config != null) {
            config.setConfigValue(newToken);
            systemConfigMapper.updateById(config);
        } else {
            config = new SystemConfigPO();
            config.setConfigKey(AlertConstants.CONFIG_KEY_TOKEN);
            config.setConfigValue(newToken);
            config.setConfigGroup(AlertConstants.CONFIG_GROUP);
            config.setDescription("夜莺告警Webhook接入Token");
            systemConfigMapper.insert(config);
        }

        AlertTokenOutput output = new AlertTokenOutput();
        output.setToken(newToken);
        output.setWebhookUrl(baseUrl + "/api/open/alert/nightingale/webhook?token=" + newToken);
        return output;
    }

    // ==================== 告警规则映射 CRUD ====================

    public PageOutput<AlertRuleMappingOutput> getMappingPage(AlertRuleMappingPageInput input) {
        Page<AlertRuleMappingPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        LambdaQueryWrapper<AlertRuleMappingPO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(input.getRuleName())) {
            wrapper.like(AlertRuleMappingPO::getRuleName, input.getRuleName());
        }
        if (input.getEnabled() != null) {
            wrapper.eq(AlertRuleMappingPO::getEnabled, input.getEnabled());
        }
        wrapper.orderByDesc(AlertRuleMappingPO::getCreateTime);

        IPage<AlertRuleMappingPO> result = alertRuleMappingMapper.selectPage(page, wrapper);

        List<AlertRuleMappingPO> records = result.getRecords();
        Set<Long> categoryIds = records.stream()
                .map(AlertRuleMappingPO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = records.stream()
                .map(AlertRuleMappingPO::getAssigneeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<TicketCategoryPO> categories = categoryMapper.selectBatchIds(categoryIds);
            for (TicketCategoryPO cat : categories) {
                categoryNameMap.put(cat.getId(), cat.getName());
            }
        }

        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUserPO> users = userMapper.selectBatchIds(userIds);
            for (SysUserPO user : users) {
                userNameMap.put(user.getId(), user.getName() != null ? user.getName() : "");
            }
        }

        List<AlertRuleMappingOutput> outputList = records.stream()
                .map(po -> toMappingOutput(po, categoryNameMap, userNameMap))
                .collect(Collectors.toList());

        return PageOutput.of(outputList, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createMapping(AlertRuleMappingCreateInput input) {
        AlertRuleMappingPO po = new AlertRuleMappingPO();
        po.setRuleName(input.getRuleName());
        po.setMatchMode(input.getMatchMode() != null ? input.getMatchMode() : AlertMatchMode.EXACT.getCode());
        po.setCategoryId(input.getCategoryId());
        po.setPriorityP1(input.getPriorityP1() != null ? input.getPriorityP1() : "urgent");
        po.setPriorityP2(input.getPriorityP2() != null ? input.getPriorityP2() : "high");
        po.setPriorityP3(input.getPriorityP3() != null ? input.getPriorityP3() : "medium");
        po.setAssigneeId(input.getAssigneeId());
        po.setDedupWindowMinutes(input.getDedupWindowMinutes() != null ? input.getDedupWindowMinutes() : 30);
        po.setEnabled(input.getEnabled() != null ? input.getEnabled() : true);

        alertRuleMappingMapper.insert(po);
        return po.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMapping(AlertRuleMappingUpdateInput input) {
        AlertRuleMappingPO existing = alertRuleMappingMapper.selectById(input.getId());
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "映射配置不存在");
        }

        if (StringUtils.hasText(input.getRuleName())) {
            existing.setRuleName(input.getRuleName());
        }
        if (StringUtils.hasText(input.getMatchMode())) {
            existing.setMatchMode(input.getMatchMode());
        }
        if (input.getCategoryId() != null) {
            existing.setCategoryId(input.getCategoryId());
        }
        if (input.getPriorityP1() != null) {
            existing.setPriorityP1(input.getPriorityP1());
        }
        if (input.getPriorityP2() != null) {
            existing.setPriorityP2(input.getPriorityP2());
        }
        if (input.getPriorityP3() != null) {
            existing.setPriorityP3(input.getPriorityP3());
        }
        if (input.getAssigneeId() != null) {
            existing.setAssigneeId(input.getAssigneeId());
        }
        if (input.getDedupWindowMinutes() != null) {
            existing.setDedupWindowMinutes(input.getDedupWindowMinutes());
        }
        if (input.getEnabled() != null) {
            existing.setEnabled(input.getEnabled());
        }

        alertRuleMappingMapper.updateById(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMapping(Long id) {
        AlertRuleMappingPO existing = alertRuleMappingMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "映射配置不存在");
        }
        alertRuleMappingMapper.deleteById(id);
    }

    public AlertRuleMappingOutput getMappingDetail(Long id) {
        AlertRuleMappingPO po = alertRuleMappingMapper.selectById(id);
        if (po == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "映射配置不存在");
        }

        Map<Long, String> categoryNameMap = new HashMap<>();
        if (po.getCategoryId() != null) {
            TicketCategoryPO cat = categoryMapper.selectById(po.getCategoryId());
            if (cat != null) {
                categoryNameMap.put(cat.getId(), cat.getName());
            }
        }

        Map<Long, String> userNameMap = new HashMap<>();
        if (po.getAssigneeId() != null) {
            SysUserPO user = userMapper.selectById(po.getAssigneeId());
            if (user != null) {
                userNameMap.put(user.getId(), user.getName() != null ? user.getName() : "");
            }
        }

        return toMappingOutput(po, categoryNameMap, userNameMap);
    }

    // ==================== 告警事件日志查询 ====================

    public PageOutput<AlertEventLogOutput> getEventLogPage(AlertEventLogPageInput input) {
        Page<AlertEventLogPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        LambdaQueryWrapper<AlertEventLogPO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(input.getRuleName())) {
            wrapper.like(AlertEventLogPO::getRuleName, input.getRuleName());
        }
        if (StringUtils.hasText(input.getTargetIdent())) {
            wrapper.like(AlertEventLogPO::getTargetIdent, input.getTargetIdent());
        }
        if (StringUtils.hasText(input.getProcessResult())) {
            wrapper.eq(AlertEventLogPO::getProcessResult, input.getProcessResult());
        }
        wrapper.orderByDesc(AlertEventLogPO::getCreateTime);

        IPage<AlertEventLogPO> result = alertEventLogMapper.selectPage(page, wrapper);

        Set<Long> ticketIds = result.getRecords().stream()
                .map(AlertEventLogPO::getTicketId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> ticketNoMap = new HashMap<>();
        if (!ticketIds.isEmpty()) {
            List<TicketPO> tickets = ticketMapper.selectBatchIds(ticketIds);
            for (TicketPO ticket : tickets) {
                ticketNoMap.put(ticket.getId(), ticket.getTicketNo());
            }
        }

        List<AlertEventLogOutput> outputList = result.getRecords().stream()
                .map(po -> toEventLogOutput(po, ticketNoMap))
                .collect(Collectors.toList());

        return PageOutput.of(outputList, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    // ==================== 转换方法 ====================

    private AlertRuleMappingOutput toMappingOutput(AlertRuleMappingPO po,
                                                    Map<Long, String> categoryNameMap,
                                                    Map<Long, String> userNameMap) {
        AlertRuleMappingOutput output = new AlertRuleMappingOutput();
        output.setId(po.getId());
        output.setRuleName(po.getRuleName());
        output.setMatchMode(po.getMatchMode());
        output.setCategoryId(po.getCategoryId());
        output.setCategoryName(categoryNameMap.getOrDefault(po.getCategoryId(), ""));
        output.setPriorityP1(po.getPriorityP1());
        output.setPriorityP2(po.getPriorityP2());
        output.setPriorityP3(po.getPriorityP3());
        output.setAssigneeId(po.getAssigneeId());
        output.setAssigneeName(po.getAssigneeId() != null
                ? userNameMap.getOrDefault(po.getAssigneeId(), "") : "");
        output.setDedupWindowMinutes(po.getDedupWindowMinutes());
        output.setEnabled(po.getEnabled());
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());
        return output;
    }

    private AlertEventLogOutput toEventLogOutput(AlertEventLogPO po, Map<Long, String> ticketNoMap) {
        AlertEventLogOutput output = new AlertEventLogOutput();
        output.setId(po.getId());
        output.setEventHash(po.getEventHash());
        output.setRuleId(po.getRuleId());
        output.setRuleName(po.getRuleName());
        output.setSeverity(po.getSeverity());
        output.setTargetIdent(po.getTargetIdent());
        output.setTriggerValue(po.getTriggerValue());
        output.setTriggerTime(po.getTriggerTime());
        output.setIsRecovered(po.getIsRecovered());
        output.setTicketId(po.getTicketId());
        output.setTicketNo(po.getTicketId() != null
                ? ticketNoMap.getOrDefault(po.getTicketId(), "") : "");
        output.setProcessResult(po.getProcessResult());
        output.setCreateTime(po.getCreateTime());
        return output;
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "未知";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp * 1000));
    }
}
