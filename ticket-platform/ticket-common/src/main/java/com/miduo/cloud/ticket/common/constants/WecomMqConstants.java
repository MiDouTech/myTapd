package com.miduo.cloud.ticket.common.constants;

/**
 * 企微回调消息队列常量
 */
public final class WecomMqConstants {

    private WecomMqConstants() {
    }

    public static final String CALLBACK_EXCHANGE = "wecom.callback.exchange";
    public static final String CALLBACK_QUEUE = "wecom.callback.queue";
    public static final String CALLBACK_ROUTING_KEY = "wecom.callback.message";
}
