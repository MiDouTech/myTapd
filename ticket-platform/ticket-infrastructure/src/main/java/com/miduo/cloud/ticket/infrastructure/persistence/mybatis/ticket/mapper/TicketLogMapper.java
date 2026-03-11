package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TicketLogMapper extends BaseMapper<TicketLogPO> {

    /**
     * 按工单ID查询变更历史（时间倒序）
     * changeType 通过 JSON_UNQUOTE(JSON_EXTRACT(remark, '$.changeType')) 匹配
     * fieldName 过滤在 Service 层内存完成，此处不下推 SQL
     *
     * @param ticketId   工单ID
     * @param changeType 变更类型 code（null 表示不过滤）
     */
    List<TicketLogPO> selectChangeHistoryByTicketId(@Param("ticketId") Long ticketId,
                                                    @Param("changeType") String changeType);
}
