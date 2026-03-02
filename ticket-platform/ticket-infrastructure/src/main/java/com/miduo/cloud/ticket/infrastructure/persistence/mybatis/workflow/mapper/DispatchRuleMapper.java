package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.DispatchRulePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分派规则Mapper
 */
@Mapper
public interface DispatchRuleMapper extends BaseMapper<DispatchRulePO> {
}
