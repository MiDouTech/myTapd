package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomPendingImagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 企微图片消息暂存 Mapper
 */
@Mapper
public interface WecomPendingImageMapper extends BaseMapper<WecomPendingImagePO> {

    /**
     * 查询同一群+同一用户在时间窗口内状态为 PENDING 的图片列表
     */
    List<WecomPendingImagePO> selectPendingByUserAndChat(
            @Param("chatId") String chatId,
            @Param("fromUserId") String fromUserId,
            @Param("windowStart") Date windowStart
    );

    /**
     * 查询超过 expireTime 且状态为 PENDING 的记录列表
     */
    List<WecomPendingImagePO> selectExpiredPending(@Param("now") Date now);
}
