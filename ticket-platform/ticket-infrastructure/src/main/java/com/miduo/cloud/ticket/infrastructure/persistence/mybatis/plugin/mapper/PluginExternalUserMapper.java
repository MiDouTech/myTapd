package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.plugin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.plugin.po.PluginExternalUserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 插件外部用户映射 Mapper
 */
@Mapper
public interface PluginExternalUserMapper extends BaseMapper<PluginExternalUserPO> {
}
