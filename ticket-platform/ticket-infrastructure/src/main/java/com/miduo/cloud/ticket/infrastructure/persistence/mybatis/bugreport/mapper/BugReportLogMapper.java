package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportLogPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BugReportLogMapper extends BaseMapper<BugReportLogPO> {

    @Delete("DELETE FROM bug_report_log WHERE report_id = #{reportId}")
    int hardDeleteByReportId(@Param("reportId") Long reportId);
}
