package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomBotMessageLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企微机器人消息日志Mapper
 */
@Mapper
public interface WecomBotMessageLogMapper extends BaseMapper<WecomBotMessageLogPO> {
}
