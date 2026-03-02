package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SLA预警级别枚举
 * 根据剩余时间百分比判断预警等级
 */
@Getter
@AllArgsConstructor
public enum SlaLevel {

    GREEN("GREEN", "正常", "剩余时间>50%"),
    YELLOW("YELLOW", "预警", "剩余时间25%~50%"),
    ORANGE("ORANGE", "告警", "剩余时间<25%"),
    RED("RED", "超时", "已超时");

    private final String code;
    private final String label;
    private final String description;

    /**
     * 根据剩余时间百分比计算SLA预警级别
     *
     * @param remainingPct 剩余时间百分比（0~100）
     * @return SLA预警级别
     */
    public static SlaLevel fromRemainingPct(int remainingPct) {
        if (remainingPct <= 0) {
            return RED;
        } else if (remainingPct < 25) {
            return ORANGE;
        } else if (remainingPct <= 50) {
            return YELLOW;
        } else {
            return GREEN;
        }
    }
}
