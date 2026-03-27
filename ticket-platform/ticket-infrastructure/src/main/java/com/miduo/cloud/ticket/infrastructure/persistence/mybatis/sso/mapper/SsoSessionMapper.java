package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sso.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sso.po.SsoSessionPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 米多 SSO 会话 Mapper
 */
@Mapper
public interface SsoSessionMapper extends BaseMapper<SsoSessionPO> {
}
