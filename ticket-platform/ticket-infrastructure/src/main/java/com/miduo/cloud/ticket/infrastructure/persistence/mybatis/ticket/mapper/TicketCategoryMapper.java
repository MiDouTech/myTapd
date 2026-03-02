package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单分类Mapper
 */
@Mapper
public interface TicketCategoryMapper extends BaseMapper<TicketCategoryPO> {
}
