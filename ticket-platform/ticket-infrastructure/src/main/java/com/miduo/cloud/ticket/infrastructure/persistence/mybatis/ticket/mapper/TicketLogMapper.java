package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单操作日志Mapper
 */
@Mapper
public interface TicketLogMapper extends BaseMapper<TicketLogPO> {
}
