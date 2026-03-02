package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomGroupBindingPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企微群绑定配置Mapper
 */
@Mapper
public interface WecomGroupBindingMapper extends BaseMapper<WecomGroupBindingPO> {
}
