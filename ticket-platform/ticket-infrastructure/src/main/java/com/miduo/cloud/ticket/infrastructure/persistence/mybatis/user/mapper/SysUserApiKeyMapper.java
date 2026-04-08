package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserApiKeyPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 API 密钥 Mapper
 */
@Mapper
public interface SysUserApiKeyMapper extends BaseMapper<SysUserApiKeyPO> {
}
