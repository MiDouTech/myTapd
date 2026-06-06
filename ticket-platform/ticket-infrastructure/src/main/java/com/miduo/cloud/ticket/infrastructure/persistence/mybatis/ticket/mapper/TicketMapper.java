package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.model.UserTicketLoadStat;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TicketMapper extends BaseMapper<TicketPO> {

    IPage<TicketPO> selectTicketPage(Page<TicketPO> page,
                                     @Param("view") String view,
                                     @Param("currentUserId") Long currentUserId,
                                     @Param("keyword") String keyword,
                                     @Param("ticketNo") String ticketNo,
                                     @Param("title") String title,
                                     @Param("companyName") String companyName,
                                     @Param("categoryId") Long categoryId,
                                     @Param("categoryGroupIds") List<Long> categoryGroupIds,
                                     @Param("statusList") List<String> statusList,
                                     @Param("priority") String priority,
                                     @Param("creatorId") Long creatorId,
                                     @Param("assigneeId") Long assigneeId,
                                     @Param("createTimeStart") String createTimeStart,
                                     @Param("createTimeEnd") String createTimeEnd,
                                     @Param("orderBy") String orderBy,
                                     @Param("asc") boolean asc,
                                     @Param("slaStatus") String slaStatus,
                                     @Param("linkableForBugReport") Boolean linkableForBugReport,
                                     @Param("viewCategoryIds") List<Long> viewCategoryIds,
                                     @Param("excludeCategoryIds") List<Long> excludeCategoryIds,
                                     @Param("alertCategoryIds") List<Long> alertCategoryIds,
                                     @Param("alertSource") String alertSource,
                                     @Param("excludeAlertSource") Boolean excludeAlertSource);

    List<Long> selectFollowedTicketIds(@Param("userId") Long userId);

    List<Long> selectParticipatedTicketIds(@Param("userId") Long userId);

    /**
     * 批量统计多个用户的未完成工单数（负载均衡分派使用，避免 N+1 查询）
     *
     * @param userIds        用户ID列表
     * @param excludeStatuses 排除的终态状态码列表（如 completed、closed）
     * @return 每个用户的未完成工单数列表
     */
    List<UserTicketLoadStat> selectActiveCountByUserIds(
            @Param("userIds") List<Long> userIds,
            @Param("excludeStatuses") List<String> excludeStatuses);

    /**
     * 人工催办成功时累计次数 +1（原子更新）
     *
     * @return 影响行数，期望为 1
     */
    int incrementUrgeCount(@Param("id") Long id);

    /**
     * 开放接口：分页拉取工单全量数据
     */
    IPage<TicketPO> selectOpenTicketExportPage(Page<TicketPO> page,
                                               @Param("createTimeStart") String createTimeStart,
                                               @Param("createTimeEnd") String createTimeEnd,
                                               @Param("completeTimeStart") String completeTimeStart,
                                               @Param("completeTimeEnd") String completeTimeEnd,
                                               @Param("statusList") List<String> statusList,
                                               @Param("businessTypeId") Long businessTypeId,
                                               @Param("businessTypeName") String businessTypeName);
}
