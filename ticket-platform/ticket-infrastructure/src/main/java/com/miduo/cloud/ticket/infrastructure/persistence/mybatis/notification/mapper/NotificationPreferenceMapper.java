package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po.NotificationPreferencePO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 通知偏好Mapper
 */
@Mapper
public interface NotificationPreferenceMapper extends BaseMapper<NotificationPreferencePO> {

    /**
     * 查询用户所有通知偏好
     */
    List<NotificationPreferencePO> selectByUserId(Long userId);
}
