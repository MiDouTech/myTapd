package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportAttachmentPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BugReportAttachmentMapper extends BaseMapper<BugReportAttachmentPO> {

    @Delete("DELETE FROM bug_report_attachment WHERE report_id = #{reportId}")
    int hardDeleteByReportId(@Param("reportId") Long reportId);
}
