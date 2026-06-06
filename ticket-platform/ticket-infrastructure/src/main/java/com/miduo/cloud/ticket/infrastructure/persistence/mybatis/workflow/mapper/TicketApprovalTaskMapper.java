package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.TicketApprovalTaskPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单审批任务 Mapper
 */
@Mapper
public interface TicketApprovalTaskMapper extends BaseMapper<TicketApprovalTaskPO> {

    /**
     * 查询指定工单当前（未删除）的所有审批任务
     */
    @Select("SELECT * FROM ticket_approval_task WHERE ticket_id = #{ticketId} AND deleted = 0 ORDER BY sort_order ASC, id ASC")
    List<TicketApprovalTaskPO> selectByTicketId(@Param("ticketId") Long ticketId);

    /**
     * 查询指定工单指定 transition 的待处理任务数量（用于幂等判断）
     */
    @Select("SELECT COUNT(*) FROM ticket_approval_task "
            + "WHERE ticket_id = #{ticketId} AND transition_id = #{transitionId} "
            + "AND task_status IN ('pending', 'waiting') AND deleted = 0")
    long countActiveByTicketAndTransition(@Param("ticketId") Long ticketId, @Param("transitionId") String transitionId);

    /**
     * 查询指定工单同一节点 pending 状态的任务数量（会签判断用）
     */
    @Select("SELECT COUNT(*) FROM ticket_approval_task "
            + "WHERE ticket_id = #{ticketId} AND node_key = #{nodeKey} "
            + "AND task_status = 'pending' AND deleted = 0")
    long countPendingByNodeKey(@Param("ticketId") Long ticketId, @Param("nodeKey") String nodeKey);

    /**
     * 查询某人的待审批任务数量（角标用）
     */
    @Select("SELECT COUNT(*) FROM ticket_approval_task WHERE assignee_id = #{assigneeId} AND task_status = 'pending' AND deleted = 0")
    long countPendingByAssignee(@Param("assigneeId") Long assigneeId);

    /**
     * 分页查询某人的待审批任务（列表页用）
     */
    List<TicketApprovalTaskPO> selectPendingPageByAssignee(@Param("assigneeId") Long assigneeId,
                                                            @Param("offset") int offset,
                                                            @Param("limit") int limit);

    /**
     * 查询同一节点下 waiting 状态中 sortOrder 最小的任务（sequential 推进用）
     */
    @Select("SELECT * FROM ticket_approval_task "
            + "WHERE ticket_id = #{ticketId} AND node_key = #{nodeKey} "
            + "AND task_status = 'waiting' AND deleted = 0 "
            + "ORDER BY sort_order ASC, id ASC LIMIT 1")
    TicketApprovalTaskPO selectNextWaitingByNodeKey(@Param("ticketId") Long ticketId, @Param("nodeKey") String nodeKey);
}
