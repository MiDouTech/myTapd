package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCommentPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketCommentMapper extends BaseMapper<TicketCommentPO> {
}
