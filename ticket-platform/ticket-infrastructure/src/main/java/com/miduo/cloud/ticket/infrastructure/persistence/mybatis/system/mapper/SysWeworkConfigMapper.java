package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SysWeworkConfigPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企业微信配置Mapper
 */
@Mapper
public interface SysWeworkConfigMapper extends BaseMapper<SysWeworkConfigPO> {
}
