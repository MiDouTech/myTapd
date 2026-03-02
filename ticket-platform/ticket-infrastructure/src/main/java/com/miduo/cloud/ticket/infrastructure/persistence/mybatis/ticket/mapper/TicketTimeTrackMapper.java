package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketTimeTrackPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单时间追踪 Mapper
 */
@Mapper
public interface TicketTimeTrackMapper extends BaseMapper<TicketTimeTrackPO> {
}
