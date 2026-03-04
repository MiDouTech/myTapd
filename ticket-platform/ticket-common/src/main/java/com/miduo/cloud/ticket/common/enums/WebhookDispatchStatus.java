package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Webhook分发状态
 */
@Getter
@AllArgsConstructor
public enum WebhookDispatchStatus {

    SUCCESS("SUCCESS", "推送成功"),
    FAIL("FAIL", "推送失败"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String label;
}
