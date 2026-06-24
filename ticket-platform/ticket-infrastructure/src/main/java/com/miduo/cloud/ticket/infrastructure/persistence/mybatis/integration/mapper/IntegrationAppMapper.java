package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.integration.po.IntegrationAppPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 插件接入应用 Mapper
 */
@Mapper
public interface IntegrationAppMapper extends BaseMapper<IntegrationAppPO> {

    IntegrationAppPO selectByAppKey(@Param("appKey") String appKey);

    IntegrationAppPO selectBySystemCode(@Param("systemCode") String systemCode);
}
