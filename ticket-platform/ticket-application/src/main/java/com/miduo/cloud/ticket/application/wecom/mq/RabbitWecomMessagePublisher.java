package com.miduo.cloud.ticket.application.wecom.mq;

import com.miduo.cloud.ticket.common.constants.WecomMqConstants;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomCallbackMessageDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 企微消息RabbitMQ发布器
 */
@Component
@ConditionalOnProperty(name = "wecom.mq.enabled", havingValue = "true")
public class RabbitWecomMessagePublisher implements WecomMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitWecomMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(WecomCallbackMessageDTO message) {
        rabbitTemplate.convertAndSend(
                WecomMqConstants.CALLBACK_EXCHANGE,
                WecomMqConstants.CALLBACK_ROUTING_KEY,
                message
        );
    }
}
