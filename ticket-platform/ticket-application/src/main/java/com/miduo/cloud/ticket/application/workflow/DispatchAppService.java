package com.miduo.cloud.ticket.application.workflow;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.DispatchStrategy;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.model.UserTicketLoadStat;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.DispatchRuleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.HandlerGroupMemberMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.DispatchRulePO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.HandlerGroupMemberPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分派应用服务
 * 支持策略：MANUAL / CATEGORY_DEFAULT / ROUND_ROBIN / LOAD_BALANCE / MATRIX
 * 核心优化：负载均衡使用批量查询，消除 N+1 问题
 */
@Service
public class DispatchAppService extends BaseApplicationService {

    private static final String ROUND_ROBIN_INDEX_KEY = "dispatch:round_robin:group:";

    /** 终态状态码（不参与负载统计） */
    private static final List<String> TERMINAL_STATUSES = Arrays.asList(
            TicketStatus.COMPLETED.getCode(),
            TicketStatus.CLOSED.getCode(),
            TicketStatus.REJECTED.getCode()
    );

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final DispatchRuleMapper dispatchRuleMapper;
    private final HandlerGroupMemberMapper handlerGroupMemberMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final TicketWorkflowAppService ticketWorkflowAppService;

    public DispatchAppService(TicketMapper ticketMapper,
                               TicketCategoryMapper ticketCategoryMapper,
                               DispatchRuleMapper dispatchRuleMapper,
                               HandlerGroupMemberMapper handlerGroupMemberMapper,
                               StringRedisTemplate stringRedisTemplate,
                               ApplicationEventPublisher eventPublisher,
                               TicketWorkflowAppService ticketWorkflowAppService) {
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.dispatchRuleMapper = dispatchRuleMapper;
        this.handlerGroupMemberMapper = handlerGroupMemberMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.eventPublisher = eventPublisher;
        this.ticketWorkflowAppService = ticketWorkflowAppService;
    }

    /**
     * 自动分派工单（按分派规则执行对应策略）
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoDispatch(Long ticketId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }

        Long categoryId = ticket.getCategoryId();
        if (categoryId == null) {
            log.warn("工单[{}]未关联分类，跳过自动分派", ticketId);
            return;
        }

        DispatchRulePO rule = findMatchingRule(categoryId);
        if (rule == null) {
            log.info("工单[{}]未找到匹配的分派规则，使用分类默认分派", ticketId);
            dispatchByCategoryDefault(ticket, categoryId);
            return;
        }

        DispatchStrategy strategy = DispatchStrategy.fromCode(rule.getStrategy());
        if (strategy == null) {
            log.warn("未知的分派策略: {}", rule.getStrategy());
            return;
        }

        switch (strategy) {
            case CATEGORY_DEFAULT:
                dispatchByCategoryDefault(ticket, categoryId);
                break;
            case ROUND_ROBIN:
                dispatchByRoundRobin(ticket, rule.getTargetGroupId());
                break;
            case LOAD_BALANCE:
                dispatchByLoadBalance(ticket, rule.getTargetGroupId());
                break;
            case MATRIX:
                dispatchByMatrix(ticket, rule);
                break;
            default:
                log.info("分派策略[{}]为手动分派，不执行自动分配", strategy.getLabel());
                break;
        }
    }

    /**
     * 分类默认分派：取分类绑定默认处理组的第一个成员（组长）
     */
    private void dispatchByCategoryDefault(TicketPO ticket, Long categoryId) {
        TicketCategoryPO category = ticketCategoryMapper.selectById(categoryId);
        if (category == null || category.getDefaultGroupId() == null) {
            log.info("分类[{}]未配置默认处理组，跳过自动分派", categoryId);
            return;
        }

        List<Long> memberIds = getGroupMemberIds(category.getDefaultGroupId());
        if (memberIds.isEmpty()) {
            log.warn("处理组[{}]没有成员，跳过自动分派", category.getDefaultGroupId());
            return;
        }

        assignTicket(ticket, memberIds.get(0), DispatchStrategy.CATEGORY_DEFAULT.getCode());
    }

    /**
     * 轮询分派：通过 Redis 计数器在处理组内轮流分配
     */
    private void dispatchByRoundRobin(TicketPO ticket, Long groupId) {
        if (groupId == null) {
            log.warn("轮询分派未配置目标处理组，跳过分派");
            return;
        }

        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            log.warn("处理组[{}]没有成员，跳过分派", groupId);
            return;
        }

        String redisKey = ROUND_ROBIN_INDEX_KEY + groupId;
        Long index = stringRedisTemplate.opsForValue().increment(redisKey);
        if (index == null) {
            index = 0L;
        }
        int idx = (int) ((index - 1) % memberIds.size());
        assignTicket(ticket, memberIds.get(idx), DispatchStrategy.ROUND_ROBIN.getCode());
    }

    /**
     * 负载均衡分派：分配给当前未完成工单最少的处理人
     * 使用批量查询（一次 SQL GROUP BY），消除 N+1 问题
     */
    private void dispatchByLoadBalance(TicketPO ticket, Long groupId) {
        if (groupId == null) {
            log.warn("负载均衡分派未配置目标处理组，跳过分派");
            return;
        }

        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            log.warn("处理组[{}]没有成员，跳过分派", groupId);
            return;
        }

        // 批量查询所有组员的活跃工单数（一次查询，消除 N+1）
        List<UserTicketLoadStat> loadStats = ticketMapper.selectActiveCountByUserIds(
                memberIds, TERMINAL_STATUSES);

        // 构建 userId → activeCount 的映射
        Map<Long, Long> loadMap = loadStats.stream()
                .collect(Collectors.toMap(UserTicketLoadStat::getUserId, UserTicketLoadStat::getActiveCount));

        // 找出工单数最少的成员
        Long minLoadUserId = memberIds.stream()
                .min((a, b) -> {
                    long countA = loadMap.getOrDefault(a, 0L);
                    long countB = loadMap.getOrDefault(b, 0L);
                    return Long.compare(countA, countB);
                })
                .orElse(null);

        if (minLoadUserId != null) {
            assignTicket(ticket, minLoadUserId, DispatchStrategy.LOAD_BALANCE.getCode());
        }
    }

    /**
     * 矩阵分派：根据工单属性（优先级/来源/分类等）匹配不同处理组/人
     * skill_match_config 格式：
     * {
     *   "matchField": "priority",
     *   "rules": [
     *     {"value": "URGENT", "groupId": 1, "userId": null},
     *     {"value": "HIGH",   "groupId": 2, "userId": null}
     *   ],
     *   "fallbackGroupId": 3
     * }
     */
    private void dispatchByMatrix(TicketPO ticket, DispatchRulePO rule) {
        String skillMatchConfig = rule.getSkillMatchConfig();
        if (skillMatchConfig == null || skillMatchConfig.isEmpty()) {
            log.warn("矩阵分派规则[{}]未配置 skill_match_config，降级为分类默认分派", rule.getId());
            dispatchByCategoryDefault(ticket, ticket.getCategoryId());
            return;
        }

        JSONObject config = JSON.parseObject(skillMatchConfig);
        String matchField = config.getString("matchField");
        JSONArray rules = config.getJSONArray("rules");
        Long fallbackGroupId = config.getLong("fallbackGroupId");

        String fieldValue = resolveTicketField(ticket, matchField);

        Long targetGroupId = null;
        Long targetUserId = null;

        if (rules != null && fieldValue != null) {
            for (int i = 0; i < rules.size(); i++) {
                JSONObject r = rules.getJSONObject(i);
                if (fieldValue.equalsIgnoreCase(r.getString("value"))) {
                    targetGroupId = r.getLong("groupId");
                    targetUserId = r.getLong("userId");
                    break;
                }
            }
        }

        // 指定了具体用户，直接分派
        if (targetUserId != null) {
            assignTicket(ticket, targetUserId, DispatchStrategy.MATRIX.getCode());
            return;
        }

        // 指定了处理组，取组内第一个成员（可扩展为轮询）
        Long groupId = targetGroupId != null ? targetGroupId : fallbackGroupId;
        if (groupId != null) {
            List<Long> memberIds = getGroupMemberIds(groupId);
            if (!memberIds.isEmpty()) {
                assignTicket(ticket, memberIds.get(0), DispatchStrategy.MATRIX.getCode());
                return;
            }
        }

        log.warn("矩阵分派未找到匹配规则，工单[{}] matchField={} value={}",
                ticket.getId(), matchField, fieldValue);
    }

    /**
     * 从工单中提取矩阵分派匹配字段值
     */
    private String resolveTicketField(TicketPO ticket, String fieldName) {
        if (fieldName == null) {
            return null;
        }
        switch (fieldName.toLowerCase()) {
            case "priority":    return ticket.getPriority();
            case "source":      return ticket.getSource();
            case "category_id": return ticket.getCategoryId() != null
                    ? ticket.getCategoryId().toString() : null;
            default:
                return null;
        }
    }

    private DispatchRulePO findMatchingRule(Long categoryId) {
        LambdaQueryWrapper<DispatchRulePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DispatchRulePO::getCategoryId, categoryId)
                .eq(DispatchRulePO::getIsActive, 1)
                .orderByAsc(DispatchRulePO::getPriorityOrder)
                .last("LIMIT 1");
        return dispatchRuleMapper.selectOne(wrapper);
    }

    private List<Long> getGroupMemberIds(Long groupId) {
        LambdaQueryWrapper<HandlerGroupMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandlerGroupMemberPO::getGroupId, groupId);
        return handlerGroupMemberMapper.selectList(wrapper).stream()
                .map(HandlerGroupMemberPO::getUserId)
                .collect(Collectors.toList());
    }

    private void assignTicket(TicketPO ticket, Long assigneeId, String assignType) {
        if (TicketStatus.fromCode(ticket.getStatus()) == TicketStatus.PENDING_ASSIGN) {
            ticketWorkflowAppService.assignFromPendingDispatch(ticket.getId(), assigneeId, null, null);
            log.info("工单[{}]分派给用户[{}]，策略: {}（含状态流转）", ticket.getId(), assigneeId, assignType);
            return;
        }

        Long previousAssigneeId = ticket.getAssigneeId();
        ticket.setAssigneeId(assigneeId);
        ticketMapper.updateById(ticket);

        log.info("工单[{}]分派给用户[{}]，策略: {}", ticket.getId(), assigneeId, assignType);

        eventPublisher.publishEvent(
                new TicketAssignedEvent(ticket.getId(), assigneeId,
                        previousAssigneeId, null, assignType));
    }
}
