package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportResponsiblePO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BugReportResponsibleMapper extends BaseMapper<BugReportResponsiblePO> {

    @Delete("DELETE FROM bug_report_responsible WHERE report_id = #{reportId}")
    int hardDeleteByReportId(@Param("reportId") Long reportId);
}
