package com.miduo.cloud.ticket.application.wecom.mq;

import com.miduo.cloud.ticket.application.wecom.WecomMessageProcessor;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomCallbackMessageDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 企微消息异步发布器（无MQ降级）
 */
@Component
@ConditionalOnProperty(name = "wecom.mq.enabled", havingValue = "false", matchIfMissing = true)
public class AsyncWecomMessagePublisher implements WecomMessagePublisher {

    private final WecomMessageProcessor messageProcessor;

    public AsyncWecomMessagePublisher(WecomMessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    @Override
    public void publish(WecomCallbackMessageDTO message) {
        processAsync(message);
    }

    @Async
    public void processAsync(WecomCallbackMessageDTO message) {
        messageProcessor.process(message);
    }
}
