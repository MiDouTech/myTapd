package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷等级对应的解决 SLA 时限规则。
 */
@Getter
@AllArgsConstructor
public enum DefectSeverityResolveSlaRule {

    P0(SeverityLevel.P0.getCode(), 60, "1小时内解决或实施有效临时方案"),
    P1(SeverityLevel.P1.getCode(), 360, "6小时内解决或实施有效临时方案"),
    P2(SeverityLevel.P2.getCode(), 480, "1个工作日内解决或转为临时处理状态"),
    P3(SeverityLevel.P3.getCode(), 1440, "3个工作日内解决或转为临时处理状态"),
    P4(SeverityLevel.P4.getCode(), 1440, "3个工作日内解决或转为临时处理状态");

    private final String severityLevel;
    private final int resolveMinutes;
    private final String description;

    public static DefectSeverityResolveSlaRule fromSeverityLevel(String severityLevel) {
        if (severityLevel == null) {
            return null;
        }
        for (DefectSeverityResolveSlaRule rule : values()) {
            if (rule.severityLevel.equals(severityLevel)) {
                return rule;
            }
        }
        return null;
    }
}
