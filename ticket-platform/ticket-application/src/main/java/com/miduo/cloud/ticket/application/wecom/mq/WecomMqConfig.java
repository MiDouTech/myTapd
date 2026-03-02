package com.miduo.cloud.ticket.application.wecom.mq;

import com.miduo.cloud.ticket.common.constants.WecomMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 企微消息队列配置
 */
@Configuration
@ConditionalOnProperty(name = "wecom.mq.enabled", havingValue = "true")
public class WecomMqConfig {

    @Bean
    public Queue wecomCallbackQueue() {
        return new Queue(WecomMqConstants.CALLBACK_QUEUE, true);
    }

    @Bean
    public DirectExchange wecomCallbackExchange() {
        return new DirectExchange(WecomMqConstants.CALLBACK_EXCHANGE, true, false);
    }

    @Bean
    public Binding wecomCallbackBinding(Queue wecomCallbackQueue, DirectExchange wecomCallbackExchange) {
        return BindingBuilder.bind(wecomCallbackQueue)
                .to(wecomCallbackExchange)
                .with(WecomMqConstants.CALLBACK_ROUTING_KEY);
    }
}
