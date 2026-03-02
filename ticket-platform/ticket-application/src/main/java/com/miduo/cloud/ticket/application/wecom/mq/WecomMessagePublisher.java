package com.miduo.cloud.ticket.application.wecom.mq;

import com.miduo.cloud.ticket.entity.dto.wecom.WecomCallbackMessageDTO;

/**
 * 企微消息发布器
 */
public interface WecomMessagePublisher {

    /**
     * 发布企微回调消息
     */
    void publish(WecomCallbackMessageDTO message);
}
