package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.model.UserRoleCodePair;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserPO> {

    /**
     * 根据用户ID批量查询角色编码
     */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据部门ID列表批量查询用户
     */
    List<SysUserPO> selectByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 根据用户ID列表批量查询角色编码
     */
    List<UserRoleCodePair> selectRoleCodesByUserIds(@Param("userIds") List<Long> userIds);
}
