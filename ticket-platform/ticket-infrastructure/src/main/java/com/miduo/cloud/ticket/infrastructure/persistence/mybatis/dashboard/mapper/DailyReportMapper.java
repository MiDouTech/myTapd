package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.mapper;

import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.DailyReportStatusRow;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.DailyReportTicketRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 日报统计 Mapper
 */
@Mapper
public interface DailyReportMapper {

    /**
     * 统计指定日期范围内各状态的工单数
     */
    List<DailyReportStatusRow> selectStatusCountsByDateRange(@Param("startTime") Date startTime,
                                                             @Param("endTime") Date endTime);

    /**
     * 统计全量未关闭工单各状态数（不限日期范围，用于汇总全局待处理数据）
     */
    List<DailyReportStatusRow> selectOpenStatusCounts();

    /**
     * 统计指定日期范围内已解决的工单数
     */
    Long selectResolvedCountByDateRange(@Param("startTime") Date startTime,
                                        @Param("endTime") Date endTime);

    /**
     * 统计指定日期范围内新建工单数
     */
    Long selectCreatedCountByDateRange(@Param("startTime") Date startTime,
                                       @Param("endTime") Date endTime);

    /**
     * 查询待解决工单列表（排查中状态）
     */
    List<DailyReportTicketRow> selectTicketsByStatus(@Param("status") String status);

    /**
     * 查询挂起工单列表
     */
    List<DailyReportTicketRow> selectSuspendedTickets();

    /**
     * 统计指定日期范围内按缺陷等级分组的已解决工单数
     */
    List<DailyReportStatusRow> selectResolvedBySeverity(@Param("startTime") Date startTime,
                                                        @Param("endTime") Date endTime);

    /**
     * 统计指定日期范围内按缺陷等级分组的未解决工单数（非终态）
     */
    List<DailyReportStatusRow> selectOpenBySeverity();

    /**
     * 查询处理中（processing）的工单列表
     */
    List<DailyReportTicketRow> selectProcessingTickets();

    /**
     * 查询待简报（pending_cs_confirm/pending_verify）的工单列表
     */
    List<DailyReportTicketRow> selectPendingVerifyTickets();

    /**
     * 查询临时解决的工单列表
     */
    List<DailyReportTicketRow> selectTempResolvedTickets();

    /**
     * 统计指定日期范围内已关闭的工单数（按是否缺陷分组）
     */
    List<DailyReportStatusRow> selectClosedByDefectType(@Param("startTime") Date startTime,
                                                        @Param("endTime") Date endTime);

    /**
     * 全量反馈总数（指定日期范围内新建 + 截至当天未关闭的存量）
     */
    Long selectTotalFeedbackCount();
}
