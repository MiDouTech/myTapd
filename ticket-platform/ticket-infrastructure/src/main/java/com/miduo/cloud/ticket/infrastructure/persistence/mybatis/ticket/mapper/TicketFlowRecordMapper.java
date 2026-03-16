package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketFlowRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单流转流水 Mapper
 */
@Mapper
public interface TicketFlowRecordMapper extends BaseMapper<TicketFlowRecordPO> {

    /**
     * 查询工单流转历史（时间正序）
     */
    List<TicketFlowRecordPO> selectByTicketId(@Param("ticketId") Long ticketId);
}
