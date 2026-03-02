package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统配置Mapper
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigPO> {

    /**
     * 按分组查询配置列表
     */
    List<SystemConfigPO> selectByGroup(String configGroup);
}
