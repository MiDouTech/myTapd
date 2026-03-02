package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.HandlerGroupMemberPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 处理组成员Mapper
 */
@Mapper
public interface HandlerGroupMemberMapper extends BaseMapper<HandlerGroupMemberPO> {
}
