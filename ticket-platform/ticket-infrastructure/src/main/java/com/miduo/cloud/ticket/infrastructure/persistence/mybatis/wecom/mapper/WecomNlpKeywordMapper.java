package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomNlpKeywordPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 企微NLP关键词配置Mapper
 */
@Mapper
public interface WecomNlpKeywordMapper extends BaseMapper<WecomNlpKeywordPO> {
}
