package com.miduo.cloud.ticket.application.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.workflow.HandlerGroupCreateInput;
import com.miduo.cloud.ticket.entity.dto.workflow.HandlerGroupListOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.HandlerGroupMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.HandlerGroupMemberMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.HandlerGroupMemberPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.HandlerGroupPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 处理组应用服务
 */
@Service
public class HandlerGroupAppService extends BaseApplicationService {

    private final HandlerGroupMapper handlerGroupMapper;
    private final HandlerGroupMemberMapper handlerGroupMemberMapper;
    private final SysUserMapper sysUserMapper;

    public HandlerGroupAppService(HandlerGroupMapper handlerGroupMapper,
                                   HandlerGroupMemberMapper handlerGroupMemberMapper,
                                   SysUserMapper sysUserMapper) {
        this.handlerGroupMapper = handlerGroupMapper;
        this.handlerGroupMemberMapper = handlerGroupMemberMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 查询处理组列表
     */
    public List<HandlerGroupListOutput> listHandlerGroups() {
        LambdaQueryWrapper<HandlerGroupPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandlerGroupPO::getIsActive, 1)
                .orderByAsc(HandlerGroupPO::getId);
        List<HandlerGroupPO> groups = handlerGroupMapper.selectList(wrapper);

        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> groupIds = groups.stream()
                .map(HandlerGroupPO::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<HandlerGroupMemberPO> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.in(HandlerGroupMemberPO::getGroupId, groupIds);
        List<HandlerGroupMemberPO> allMembers = handlerGroupMemberMapper.selectList(memberWrapper);

        Map<Long, List<HandlerGroupMemberPO>> membersByGroup = allMembers.stream()
                .collect(Collectors.groupingBy(HandlerGroupMemberPO::getGroupId));

        Set<Long> allUserIds = new HashSet<>();
        for (HandlerGroupPO g : groups) {
            if (g.getLeaderId() != null) {
                allUserIds.add(g.getLeaderId());
            }
        }
        for (HandlerGroupMemberPO m : allMembers) {
            allUserIds.add(m.getUserId());
        }

        Map<Long, String> userNameMap = Collections.emptyMap();
        if (!allUserIds.isEmpty()) {
            List<SysUserPO> users = sysUserMapper.selectBatchIds(allUserIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(SysUserPO::getId, SysUserPO::getName));
        }

        List<HandlerGroupListOutput> result = new ArrayList<>();
        for (HandlerGroupPO group : groups) {
            HandlerGroupListOutput output = new HandlerGroupListOutput();
            output.setId(group.getId());
            output.setName(group.getName());
            output.setDescription(group.getDescription());
            output.setSkillTags(group.getSkillTags());
            output.setIsActive(group.getIsActive());
            output.setLeaderId(group.getLeaderId());
            output.setLeaderName(userNameMap.getOrDefault(group.getLeaderId(), ""));
            output.setCreateTime(group.getCreateTime());

            List<HandlerGroupMemberPO> groupMembers = membersByGroup.getOrDefault(
                    group.getId(), Collections.emptyList());
            output.setMemberCount(groupMembers.size());

            List<HandlerGroupListOutput.MemberItem> memberItems = new ArrayList<>();
            for (HandlerGroupMemberPO m : groupMembers) {
                HandlerGroupListOutput.MemberItem memberItem = new HandlerGroupListOutput.MemberItem();
                memberItem.setUserId(m.getUserId());
                memberItem.setUserName(userNameMap.getOrDefault(m.getUserId(), ""));
                memberItems.add(memberItem);
            }
            output.setMembers(memberItems);

            result.add(output);
        }

        return result;
    }

    /**
     * 创建处理组
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createHandlerGroup(HandlerGroupCreateInput input) {
        LambdaQueryWrapper<HandlerGroupPO> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(HandlerGroupPO::getName, input.getName());
        Long count = handlerGroupMapper.selectCount(checkWrapper);
        if (count > 0) {
            throw BusinessException.of(ErrorCode.DATA_ALREADY_EXISTS, "处理组名称已存在");
        }

        HandlerGroupPO groupPO = new HandlerGroupPO();
        groupPO.setName(input.getName());
        groupPO.setDescription(input.getDescription());
        groupPO.setSkillTags(input.getSkillTags());
        groupPO.setLeaderId(input.getLeaderId());
        groupPO.setIsActive(1);
        handlerGroupMapper.insert(groupPO);

        for (Long memberId : input.getMemberIds()) {
            HandlerGroupMemberPO memberPO = new HandlerGroupMemberPO();
            memberPO.setGroupId(groupPO.getId());
            memberPO.setUserId(memberId);
            handlerGroupMemberMapper.insert(memberPO);
        }

        return groupPO.getId();
    }
}
