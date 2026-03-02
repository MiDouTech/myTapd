package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysRolePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色Mapper接口
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRolePO> {
}
