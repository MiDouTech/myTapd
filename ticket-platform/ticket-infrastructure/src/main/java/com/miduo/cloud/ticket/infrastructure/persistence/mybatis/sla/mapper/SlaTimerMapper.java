package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaTimerPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SLA计时器Mapper
 */
@Mapper
public interface SlaTimerMapper extends BaseMapper<SlaTimerPO> {

    /**
     * 查询所有运行中的SLA计时器
     */
    List<SlaTimerPO> selectRunningTimers();

    /**
     * 批量更新计时器状态
     */
    int batchUpdateStatus(List<SlaTimerPO> timers);
}
