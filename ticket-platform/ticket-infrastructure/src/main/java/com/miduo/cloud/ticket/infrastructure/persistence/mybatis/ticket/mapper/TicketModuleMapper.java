package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketModulePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单模块 Mapper
 */
@Mapper
public interface TicketModuleMapper extends BaseMapper<TicketModulePO> {
}
