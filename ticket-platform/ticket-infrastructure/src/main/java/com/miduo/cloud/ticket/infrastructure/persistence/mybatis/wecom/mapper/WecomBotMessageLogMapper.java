package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpLogPageInput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpLogPageOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomBotMessageLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 企微机器人消息日志Mapper
 */
@Mapper
public interface WecomBotMessageLogMapper extends BaseMapper<WecomBotMessageLogPO> {

    /**
     * 分页查询NLP解析日志
     *
     * @param page  分页参数
     * @param input 查询条件
     * @return 分页结果
     */
    Page<NlpLogPageOutput> pageNlpLogs(@Param("page") Page<NlpLogPageOutput> page,
                                       @Param("input") NlpLogPageInput input);
}
