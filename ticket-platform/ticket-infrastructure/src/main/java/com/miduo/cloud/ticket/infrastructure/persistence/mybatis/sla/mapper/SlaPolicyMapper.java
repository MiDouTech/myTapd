package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaPolicyPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SLA策略Mapper
 */
@Mapper
public interface SlaPolicyMapper extends BaseMapper<SlaPolicyPO> {
}
