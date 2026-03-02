package com.miduo.cloud.ticket.application.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.Priority;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.sla.SlaPolicyCreateInput;
import com.miduo.cloud.ticket.entity.dto.sla.SlaPolicyOutput;
import com.miduo.cloud.ticket.entity.dto.sla.SlaPolicyUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.mapper.SlaPolicyMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaPolicyPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SLA策略应用服务
 */
@Service
public class SlaApplicationService extends BaseApplicationService {

    private final SlaPolicyMapper slaPolicyMapper;

    public SlaApplicationService(SlaPolicyMapper slaPolicyMapper) {
        this.slaPolicyMapper = slaPolicyMapper;
    }

    /**
     * 查询SLA策略列表
     */
    public List<SlaPolicyOutput> listPolicies() {
        LambdaQueryWrapper<SlaPolicyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SlaPolicyPO::getId);
        List<SlaPolicyPO> poList = slaPolicyMapper.selectList(wrapper);
        return poList.stream().map(this::convertToOutput).collect(Collectors.toList());
    }

    /**
     * 创建SLA策略
     */
    @Transactional(rollbackFor = Exception.class)
    public SlaPolicyOutput createPolicy(SlaPolicyCreateInput input) {
        Priority priority = Priority.fromCode(input.getPriority());
        if (priority == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "无效的优先级: " + input.getPriority());
        }

        SlaPolicyPO po = new SlaPolicyPO();
        po.setName(input.getName());
        po.setPriority(input.getPriority());
        po.setResponseTime(input.getResponseTime());
        po.setResolveTime(input.getResolveTime());
        po.setWarningPct(input.getWarningPct() != null ? input.getWarningPct() : 75);
        po.setCriticalPct(input.getCriticalPct() != null ? input.getCriticalPct() : 90);
        po.setDescription(input.getDescription());
        po.setIsActive(1);

        slaPolicyMapper.insert(po);

        log.info("SLA策略创建成功: id={}, name={}", po.getId(), po.getName());
        return convertToOutput(po);
    }

    /**
     * 更新SLA策略
     */
    @Transactional(rollbackFor = Exception.class)
    public SlaPolicyOutput updatePolicy(SlaPolicyUpdateInput input) {
        SlaPolicyPO existing = slaPolicyMapper.selectById(input.getId());
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "SLA策略不存在");
        }

        if (input.getName() != null) {
            existing.setName(input.getName());
        }
        if (input.getPriority() != null) {
            Priority priority = Priority.fromCode(input.getPriority());
            if (priority == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "无效的优先级: " + input.getPriority());
            }
            existing.setPriority(input.getPriority());
        }
        if (input.getResponseTime() != null) {
            existing.setResponseTime(input.getResponseTime());
        }
        if (input.getResolveTime() != null) {
            existing.setResolveTime(input.getResolveTime());
        }
        if (input.getWarningPct() != null) {
            existing.setWarningPct(input.getWarningPct());
        }
        if (input.getCriticalPct() != null) {
            existing.setCriticalPct(input.getCriticalPct());
        }
        if (input.getDescription() != null) {
            existing.setDescription(input.getDescription());
        }
        if (input.getIsActive() != null) {
            existing.setIsActive(input.getIsActive());
        }

        slaPolicyMapper.updateById(existing);

        log.info("SLA策略更新成功: id={}", existing.getId());
        return convertToOutput(existing);
    }

    /**
     * 根据优先级获取激活的SLA策略
     */
    public SlaPolicyPO getActivePolicyByPriority(String priority) {
        LambdaQueryWrapper<SlaPolicyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaPolicyPO::getPriority, priority)
                .eq(SlaPolicyPO::getIsActive, 1)
                .last("LIMIT 1");
        return slaPolicyMapper.selectOne(wrapper);
    }

    private SlaPolicyOutput convertToOutput(SlaPolicyPO po) {
        if (po == null) {
            return null;
        }
        SlaPolicyOutput output = new SlaPolicyOutput();
        output.setId(po.getId());
        output.setName(po.getName());
        output.setPriority(po.getPriority());
        Priority priority = Priority.fromCode(po.getPriority());
        output.setPriorityLabel(priority != null ? priority.getLabel() : po.getPriority());
        output.setResponseTime(po.getResponseTime());
        output.setResolveTime(po.getResolveTime());
        output.setWarningPct(po.getWarningPct());
        output.setCriticalPct(po.getCriticalPct());
        output.setDescription(po.getDescription());
        output.setIsActive(po.getIsActive());
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());
        return output;
    }
}
