package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.po.AlertRuleMappingPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则映射配置 Mapper
 */
@Mapper
public interface AlertRuleMappingMapper extends BaseMapper<AlertRuleMappingPO> {
}
