package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流定义Mapper
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<WorkflowPO> {
}
