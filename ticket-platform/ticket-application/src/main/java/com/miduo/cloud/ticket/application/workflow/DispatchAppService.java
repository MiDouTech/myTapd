package com.miduo.cloud.ticket.application.workflow;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.ticket.TicketAssigneeSyncService;
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
import org.springframework.transaction.annotation.Propagation;
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
    private final TicketAssigneeSyncService ticketAssigneeSyncService;

    public DispatchAppService(TicketMapper ticketMapper,
                               TicketCategoryMapper ticketCategoryMapper,
                               DispatchRuleMapper dispatchRuleMapper,
                               HandlerGroupMemberMapper handlerGroupMemberMapper,
                               StringRedisTemplate stringRedisTemplate,
                               ApplicationEventPublisher eventPublisher,
                               TicketWorkflowAppService ticketWorkflowAppService,
                               TicketAssigneeSyncService ticketAssigneeSyncService) {
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.dispatchRuleMapper = dispatchRuleMapper;
        this.handlerGroupMemberMapper = handlerGroupMemberMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.eventPublisher = eventPublisher;
        this.ticketWorkflowAppService = ticketWorkflowAppService;
        this.ticketAssigneeSyncService = ticketAssigneeSyncService;
    }

    /**
     * 自动分派工单（按分派规则执行对应策略）
     * <p>使用独立事务，避免分派失败（如处理人 ID 在成员表中存在但 sys_user 已删除）将外层
     * {@code createTicket} 事务标记为 rollback-only，从而导致企微建单等场景整单回滚。</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
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
            log.warn("未知的分派策略: {}，回退分类默认分派", rule.getStrategy());
            dispatchByCategoryDefault(ticket, categoryId);
            return;
        }

        log.debug("工单[{}]自动分派: categoryId={}, ruleId={}, strategy={}, targetGroupId={}",
                ticketId, categoryId, rule.getId(), strategy.getCode(), rule.getTargetGroupId());

        switch (strategy) {
            case MANUAL:
                // 分类上已绑定默认处理组时，新建工单仍应按组自动分派；MANUAL 表示不启用规则表中的轮询/矩阵等扩展策略
                log.debug("工单[{}]分派规则策略为 MANUAL，使用分类默认处理组分派", ticketId);
                dispatchByCategoryDefault(ticket, categoryId);
                break;
            case CATEGORY_DEFAULT:
                dispatchByCategoryDefault(ticket, categoryId);
                break;
            case ROUND_ROBIN:
                if (rule.getTargetGroupId() == null) {
                    log.info("工单[{}]轮询分派规则未配置 target_group_id，回退分类默认分派", ticketId);
                    dispatchByCategoryDefault(ticket, categoryId);
                } else {
                    dispatchByRoundRobin(ticket, rule.getTargetGroupId());
                }
                break;
            case LOAD_BALANCE:
                if (rule.getTargetGroupId() == null) {
                    log.info("工单[{}]负载均衡分派规则未配置 target_group_id，回退分类默认分派", ticketId);
                    dispatchByCategoryDefault(ticket, categoryId);
                } else {
                    dispatchByLoadBalance(ticket, rule.getTargetGroupId());
                }
                break;
            case MATRIX:
                dispatchByMatrix(ticket, rule);
                break;
        }

        // 矩阵未命中、目标组无成员等场景：仍在待分派且无处理人时，用分类默认组再试一次
        TicketPO afterRule = ticketMapper.selectById(ticketId);
        if (afterRule != null
                && TicketStatus.fromCode(afterRule.getStatus()) == TicketStatus.PENDING_ASSIGN
                && afterRule.getAssigneeId() == null) {
            log.info("工单[{}]按分派规则未产生处理人（strategy={}），回退分类默认处理组", ticketId, strategy.getCode());
            dispatchByCategoryDefault(afterRule, categoryId);
        }
    }

    /**
     * 分类默认分派：对分类绑定的默认处理组成员按 Redis 轮询序号轮流分配（与 ROUND_ROBIN 策略同一套计数器，保证组内公平）
     */
    private void dispatchByCategoryDefault(TicketPO ticket, Long categoryId) {
        TicketCategoryPO category = ticketCategoryMapper.selectById(categoryId);
        if (category == null || category.getDefaultGroupId() == null) {
            log.info("分类[{}]未配置默认处理组，跳过自动分派", categoryId);
            return;
        }

        Long chosen = pickRoundRobinAssignee(category.getDefaultGroupId());
        if (chosen == null) {
            log.warn("处理组[{}]没有成员，跳过自动分派", category.getDefaultGroupId());
            return;
        }

        assignTicket(ticket, chosen, DispatchStrategy.CATEGORY_DEFAULT.getCode());
    }

    /**
     * 轮询分派：通过 Redis 计数器在处理组内轮流分配
     */
    private void dispatchByRoundRobin(TicketPO ticket, Long groupId) {
        Long chosen = pickRoundRobinAssignee(groupId);
        if (chosen == null) {
            return;
        }
        assignTicket(ticket, chosen, DispatchStrategy.ROUND_ROBIN.getCode());
    }

    /**
     * 在处理组内按 Redis 自增序号轮询选出下一处理人；成员列表按主键升序保证稳定
     */
    private Long pickRoundRobinAssignee(Long groupId) {
        if (groupId == null) {
            log.warn("轮询分派未配置目标处理组，跳过分派");
            return null;
        }

        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            log.warn("处理组[{}]没有成员，跳过分派", groupId);
            return null;
        }

        String redisKey = ROUND_ROBIN_INDEX_KEY + groupId;
        Long index = stringRedisTemplate.opsForValue().increment(redisKey);
        if (index == null) {
            index = 0L;
        }
        int idx = (int) ((index - 1) % memberIds.size());
        return memberIds.get(idx);
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
        wrapper.eq(HandlerGroupMemberPO::getGroupId, groupId)
                .orderByAsc(HandlerGroupMemberPO::getId);
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
        ticketAssigneeSyncService.syncSingleAssigneeRow(ticket, assigneeId);
        ticketMapper.updateById(ticket);

        log.info("工单[{}]分派给用户[{}]，策略: {}", ticket.getId(), assigneeId, assignType);

        eventPublisher.publishEvent(
                new TicketAssignedEvent(ticket.getId(), assigneeId,
                        previousAssigneeId, null, assignType));
    }
}
