package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowPO> {

    @Update("UPDATE workflow "
            + "SET invocation_count = IFNULL(invocation_count, 0) + 1, "
            + "first_invoked_time = IFNULL(first_invoked_time, NOW()), "
            + "last_invoked_time = NOW() "
            + "WHERE id = #{workflowId} AND deleted = 0")
    int incrementInvocation(@Param("workflowId") Long workflowId);
}
