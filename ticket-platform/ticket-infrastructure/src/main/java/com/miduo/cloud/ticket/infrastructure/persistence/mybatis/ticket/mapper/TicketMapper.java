package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<TicketPO> {
}
