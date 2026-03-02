package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.DepartmentPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门Mapper接口
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentPO> {
}
