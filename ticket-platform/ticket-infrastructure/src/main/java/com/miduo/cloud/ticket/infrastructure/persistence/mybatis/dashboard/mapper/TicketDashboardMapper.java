package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.mapper;

import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardCategoryDistributionOutput;
import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardEfficiencyOutput;
import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardSlaAchievementOutput;
import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardWorkloadOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.DailyCountRow;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.StatusCountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 数据看板统计Mapper
 */
@Mapper
public interface TicketDashboardMapper {

    List<StatusCountRow> selectStatusCounts();

    Long selectSlaBreachedTicketCount();

    List<DailyCountRow> selectCreatedTrend(@Param("startTime") Date startTime,
                                           @Param("endTime") Date endTime);

    List<DailyCountRow> selectClosedTrend(@Param("startTime") Date startTime,
                                          @Param("endTime") Date endTime);

    Long countOpenBefore(@Param("startTime") Date startTime);

    List<DashboardCategoryDistributionOutput> selectCategoryDistribution();

    DashboardEfficiencyOutput selectEfficiency();

    DashboardSlaAchievementOutput selectSlaAchievement();

    List<DashboardWorkloadOutput> selectWorkloadTop(@Param("limit") Integer limit);
}
