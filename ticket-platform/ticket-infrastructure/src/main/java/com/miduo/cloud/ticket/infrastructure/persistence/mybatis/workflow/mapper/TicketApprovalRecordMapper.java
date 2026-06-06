package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.TicketApprovalRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工单审批操作记录 Mapper
 */
@Mapper
public interface TicketApprovalRecordMapper extends BaseMapper<TicketApprovalRecordPO> {

    /**
     * 查询工单的全部审批记录（审批时间轴用）
     */
    @Select("SELECT * FROM ticket_approval_record WHERE ticket_id = #{ticketId} AND deleted = 0 ORDER BY create_time ASC")
    List<TicketApprovalRecordPO> selectByTicketId(@Param("ticketId") Long ticketId);
}
