package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BugReportMapper extends BaseMapper<BugReportPO> {
}
