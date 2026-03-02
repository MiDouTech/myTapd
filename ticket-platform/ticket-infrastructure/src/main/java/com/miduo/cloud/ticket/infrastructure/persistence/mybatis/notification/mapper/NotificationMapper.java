package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.notification.po.NotificationPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知记录Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<NotificationPO> {

    /**
     * 查询用户未读通知数量
     */
    int countUnreadByUserId(Long userId);
}
