package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TicketMapper extends BaseMapper<TicketPO> {

    IPage<TicketPO> selectTicketPage(Page<TicketPO> page,
                                     @Param("view") String view,
                                     @Param("currentUserId") Long currentUserId,
                                     @Param("ticketNo") String ticketNo,
                                     @Param("title") String title,
                                     @Param("categoryId") Long categoryId,
                                     @Param("status") String status,
                                     @Param("priority") String priority,
                                     @Param("creatorId") Long creatorId,
                                     @Param("assigneeId") Long assigneeId,
                                     @Param("createTimeStart") String createTimeStart,
                                     @Param("createTimeEnd") String createTimeEnd,
                                     @Param("orderBy") String orderBy,
                                     @Param("asc") boolean asc);

    List<Long> selectFollowedTicketIds(@Param("userId") Long userId);

    List<Long> selectParticipatedTicketIds(@Param("userId") Long userId);
}
