package com.miduo.cloud.ticket.application.wecom.mq;

import com.miduo.cloud.ticket.application.wecom.WecomMessageProcessor;
import com.miduo.cloud.ticket.common.constants.WecomMqConstants;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomCallbackMessageDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 企微消息消费者
 */
@Component
@ConditionalOnProperty(name = "wecom.mq.enabled", havingValue = "true")
public class WecomMessageConsumer {

    private final WecomMessageProcessor messageProcessor;

    public WecomMessageConsumer(WecomMessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    @RabbitListener(queues = WecomMqConstants.CALLBACK_QUEUE)
    public void consume(WecomCallbackMessageDTO message) {
        messageProcessor.process(message);
    }
}
