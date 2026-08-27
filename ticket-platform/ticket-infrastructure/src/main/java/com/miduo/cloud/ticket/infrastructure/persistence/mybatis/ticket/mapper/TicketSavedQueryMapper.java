package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketSavedQueryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TicketSavedQueryMapper extends BaseMapper<TicketSavedQueryPO> {
    List<TicketSavedQueryPO> selectByUserId(@Param("userId") Long userId);
    TicketSavedQueryPO selectOwnedById(@Param("id") Long id, @Param("userId") Long userId);
    int countActiveByName(@Param("userId") Long userId, @Param("name") String name,
                          @Param("excludeId") Long excludeId);
    int updateOwned(TicketSavedQueryPO query);
    int softDeleteOwned(@Param("id") Long id, @Param("userId") Long userId,
                        @Param("updateBy") String updateBy);
}
