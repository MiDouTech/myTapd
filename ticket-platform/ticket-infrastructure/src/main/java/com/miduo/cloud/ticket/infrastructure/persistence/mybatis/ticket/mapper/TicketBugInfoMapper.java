package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketBugInfoPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 缺陷工单客服信息 Mapper
 */
@Mapper
public interface TicketBugInfoMapper extends BaseMapper<TicketBugInfoPO> {
}
