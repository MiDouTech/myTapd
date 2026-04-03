package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.model.WorkflowNodeObservationStat;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketNodeDurationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单节点耗时 Mapper
 */
@Mapper
public interface TicketNodeDurationMapper extends BaseMapper<TicketNodeDurationPO> {

    /**
     * 按工作流聚合节点耗时统计
     */
    List<WorkflowNodeObservationStat> selectWorkflowNodeStats(@Param("workflowId") Long workflowId);
}
