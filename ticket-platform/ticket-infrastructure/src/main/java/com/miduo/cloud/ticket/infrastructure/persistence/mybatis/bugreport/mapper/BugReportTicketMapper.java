package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportTicketPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BugReportTicketMapper extends BaseMapper<BugReportTicketPO> {

    @Delete("DELETE FROM bug_report_ticket WHERE report_id = #{reportId}")
    int hardDeleteByReportId(@Param("reportId") Long reportId);

    /**
     * 批量插入简报-工单关联
     * 避免循环单条 insert 触犯 DB 红线
     */
    int batchInsert(@Param("list") List<BugReportTicketPO> list);
}
