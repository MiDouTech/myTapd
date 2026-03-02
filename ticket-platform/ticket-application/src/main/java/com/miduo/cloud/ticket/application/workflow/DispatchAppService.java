package com.miduo.cloud.ticket.application.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.DispatchStrategy;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.common.event.TicketAssignedEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分派应用服务 - 负责工单自动分派和分派策略执行
 */
@Service
public class DispatchAppService extends BaseApplicationService {

    private static final String ROUND_ROBIN_INDEX_KEY = "dispatch:round_robin:group:";

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final DispatchRuleMapper dispatchRuleMapper;
    private final HandlerGroupMemberMapper handlerGroupMemberMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public DispatchAppService(TicketMapper ticketMapper,
                               TicketCategoryMapper ticketCategoryMapper,
                               DispatchRuleMapper dispatchRuleMapper,
                               HandlerGroupMemberMapper handlerGroupMemberMapper,
                               StringRedisTemplate stringRedisTemplate,
                               ApplicationEventPublisher eventPublisher) {
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.dispatchRuleMapper = dispatchRuleMapper;
        this.handlerGroupMemberMapper = handlerGroupMemberMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 自动分派工单
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
            default:
                log.info("分派策略[{}]暂不支持自动分派", strategy.getLabel());
                break;
        }
    }

    /**
     * 分类默认分派：根据分类绑定的默认处理组，选取组长作为处理人
     */
    private void dispatchByCategoryDefault(TicketPO ticket, Long categoryId) {
        TicketCategoryPO category = ticketCategoryMapper.selectById(categoryId);
        if (category == null || category.getDefaultGroupId() == null) {
            log.info("分类[{}]未配置默认处理组", categoryId);
            return;
        }

        Long groupId = category.getDefaultGroupId();
        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            log.warn("处理组[{}]没有成员", groupId);
            return;
        }

        Long assigneeId = memberIds.get(0);
        assignTicket(ticket, assigneeId, "CATEGORY_DEFAULT");
    }

    /**
     * 轮询分派：在处理组内按顺序轮流分配
     */
    private void dispatchByRoundRobin(TicketPO ticket, Long groupId) {
        if (groupId == null) {
            log.warn("轮询分派未配置目标处理组");
            return;
        }

        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            log.warn("处理组[{}]没有成员", groupId);
            return;
        }

        String redisKey = ROUND_ROBIN_INDEX_KEY + groupId;
        Long index = stringRedisTemplate.opsForValue().increment(redisKey);
        if (index == null) {
            index = 0L;
        }
        int idx = (int) ((index - 1) % memberIds.size());
        Long assigneeId = memberIds.get(idx);
        assignTicket(ticket, assigneeId, "ROUND_ROBIN");
    }

    /**
     * 负载均衡分派：分配给当前待办最少的处理人
     */
    private void dispatchByLoadBalance(TicketPO ticket, Long groupId) {
        if (groupId == null) {
            log.warn("负载均衡分派未配置目标处理组");
            return;
        }

        List<Long> memberIds = getGroupMemberIds(groupId);
        if (memberIds.isEmpty()) {
            log.warn("处理组[{}]没有成员", groupId);
            return;
        }

        Long minLoadUserId = null;
        long minCount = Long.MAX_VALUE;

        for (Long memberId : memberIds) {
            LambdaQueryWrapper<TicketPO> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(TicketPO::getAssigneeId, memberId)
                    .notIn(TicketPO::getStatus, "COMPLETED", "CLOSED");
            long count = ticketMapper.selectCount(countWrapper);
            if (count < minCount) {
                minCount = count;
                minLoadUserId = memberId;
            }
        }

        if (minLoadUserId != null) {
            assignTicket(ticket, minLoadUserId, "LOAD_BALANCE");
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
        List<HandlerGroupMemberPO> members = handlerGroupMemberMapper.selectList(wrapper);
        return members.stream()
                .map(HandlerGroupMemberPO::getUserId)
                .collect(Collectors.toList());
    }

    private void assignTicket(TicketPO ticket, Long assigneeId, String assignType) {
        Long previousAssigneeId = ticket.getAssigneeId();
        ticket.setAssigneeId(assigneeId);
        ticketMapper.updateById(ticket);

        log.info("工单[{}]分派给用户[{}]，策略: {}", ticket.getId(), assigneeId, assignType);

        eventPublisher.publishEvent(
                new TicketAssignedEvent(ticket.getId(), assigneeId,
                        previousAssigneeId, null, assignType));
    }
}
