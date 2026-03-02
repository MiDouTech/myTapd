package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.webhook.po.WebhookConfigPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Webhook配置Mapper
 */
@Mapper
public interface WebhookConfigMapper extends BaseMapper<WebhookConfigPO> {

    List<WebhookConfigPO> selectActiveByEventType(@Param("eventType") String eventType);
}
