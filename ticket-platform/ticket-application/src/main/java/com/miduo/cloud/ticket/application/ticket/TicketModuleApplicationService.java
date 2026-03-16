package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketModuleInput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketModuleOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketModuleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketModulePO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工单模块应用服务（管理测试信息-所属模块的自定义下拉选项）
 */
@Service
public class TicketModuleApplicationService {

    @Resource
    private TicketModuleMapper ticketModuleMapper;

    /**
     * 获取所有模块列表（按sort升序排列）
     */
    public List<TicketModuleOutput> listModules() {
        List<TicketModulePO> modules = ticketModuleMapper.selectList(
                new LambdaQueryWrapper<TicketModulePO>()
                        .orderByAsc(TicketModulePO::getSort)
                        .orderByAsc(TicketModulePO::getId)
        );
        return modules.stream().map(this::toOutput).collect(Collectors.toList());
    }

    /**
     * 创建新模块（模块名称不能重复）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createModule(TicketModuleInput input) {
        Long count = ticketModuleMapper.selectCount(
                new LambdaQueryWrapper<TicketModulePO>()
                        .eq(TicketModulePO::getName, input.getName())
        );
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.DATA_ALREADY_EXISTS, "模块名称已存在：" + input.getName());
        }

        TicketModulePO po = new TicketModulePO();
        po.setName(input.getName());
        po.setSort(0);
        ticketModuleMapper.insert(po);
        return po.getId();
    }

    /**
     * 删除模块
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModule(Long id) {
        TicketModulePO po = ticketModuleMapper.selectById(id);
        if (po == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "模块不存在");
        }
        ticketModuleMapper.deleteById(id);
    }

    private TicketModuleOutput toOutput(TicketModulePO po) {
        TicketModuleOutput output = new TicketModuleOutput();
        output.setId(po.getId());
        output.setName(po.getName());
        output.setSort(po.getSort());
        return output;
    }
}
