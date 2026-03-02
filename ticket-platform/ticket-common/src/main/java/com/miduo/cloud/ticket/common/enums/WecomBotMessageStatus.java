package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 企微机器人消息处理状态
 */
@Getter
@AllArgsConstructor
public enum WecomBotMessageStatus {

    SUCCESS("SUCCESS", "处理成功"),
    FAIL("FAIL", "处理失败"),
    DUPLICATE("DUPLICATE", "重复消息"),
    IGNORED("IGNORED", "已忽略");

    private final String code;
    private final String label;
}
