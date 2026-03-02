package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketNodeDurationPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单节点耗时 Mapper
 */
@Mapper
public interface TicketNodeDurationMapper extends BaseMapper<TicketNodeDurationPO> {
}
