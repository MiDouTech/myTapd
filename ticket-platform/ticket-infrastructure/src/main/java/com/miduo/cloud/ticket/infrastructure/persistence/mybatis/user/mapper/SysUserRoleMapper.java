package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserRolePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联Mapper接口
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRolePO> {
}
