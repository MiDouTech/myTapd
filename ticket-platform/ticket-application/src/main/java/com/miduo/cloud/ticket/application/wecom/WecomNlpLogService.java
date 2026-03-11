package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpLogPageInput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpLogPageOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomBotMessageLogMapper;
import org.springframework.stereotype.Service;

/**
 * 企微NLP解析日志查询服务
 * Task023：企微文本消息自动创建工单 - 解析日志
 */
@Service
public class WecomNlpLogService {

    private final WecomBotMessageLogMapper botMessageLogMapper;

    public WecomNlpLogService(WecomBotMessageLogMapper botMessageLogMapper) {
        this.botMessageLogMapper = botMessageLogMapper;
    }

    /**
     * 分页查询NLP解析日志（API000436）
     *
     * @param input 查询条件
     * @return 分页结果
     */
    public PageOutput<NlpLogPageOutput> pageNlpLogs(NlpLogPageInput input) {
        Page<NlpLogPageOutput> page = new Page<>(input.getPageNum(), input.getPageSize());
        Page<NlpLogPageOutput> result = botMessageLogMapper.pageNlpLogs(page, input);
        return PageOutput.of(result.getRecords(), result.getTotal(), input.getPageNum(), input.getPageSize());
    }
}
