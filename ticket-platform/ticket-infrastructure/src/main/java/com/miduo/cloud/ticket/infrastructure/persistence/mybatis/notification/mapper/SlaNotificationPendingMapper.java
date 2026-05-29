package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po.SlaNotificationPendingPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SLA 通知延迟发送队列 Mapper
 */
@Mapper
public interface SlaNotificationPendingMapper extends BaseMapper<SlaNotificationPendingPO> {
}
