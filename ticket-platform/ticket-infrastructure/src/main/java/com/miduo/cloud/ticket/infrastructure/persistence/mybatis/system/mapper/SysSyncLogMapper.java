package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SysSyncLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步日志Mapper
 */
@Mapper
public interface SysSyncLogMapper extends BaseMapper<SysSyncLogPO> {
}
