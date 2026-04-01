package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.alert.po.AlertEventLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警事件日志 Mapper
 */
@Mapper
public interface AlertEventLogMapper extends BaseMapper<AlertEventLogPO> {
}
