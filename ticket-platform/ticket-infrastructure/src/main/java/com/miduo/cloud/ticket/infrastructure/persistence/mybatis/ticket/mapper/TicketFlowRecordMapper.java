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

    /**
     * 按工作流查询最近流转记录（时间倒序）
     */
    List<TicketFlowRecordPO> selectRecentByWorkflowId(@Param("workflowId") Long workflowId,
                                                      @Param("limit") Long limit);

    /**
     * 查询最近一次“进入指定状态”的状态流转记录（排除 from=to 的纯转派）
     */
    TicketFlowRecordPO selectLatestStatusTransitionToStatus(@Param("ticketId") Long ticketId,
                                                            @Param("toStatus") String toStatus);
}
