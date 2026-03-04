package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po.WebhookDispatchLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Webhook推送明细日志Mapper
 */
@Mapper
public interface WebhookDispatchLogMapper extends BaseMapper<WebhookDispatchLogPO> {
}
