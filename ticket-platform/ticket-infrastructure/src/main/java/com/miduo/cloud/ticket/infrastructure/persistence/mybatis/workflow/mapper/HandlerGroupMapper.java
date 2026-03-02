package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.HandlerGroupPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 处理组Mapper
 */
@Mapper
public interface HandlerGroupMapper extends BaseMapper<HandlerGroupPO> {
}
