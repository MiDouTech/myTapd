package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单状态枚举
 * 通用工单 + 缺陷工单的所有状态定义
 */
@Getter
@AllArgsConstructor
public enum TicketStatus {

    PENDING_ASSIGN("pending_assign", "待分派"),
    PENDING_ACCEPT("pending_accept", "待受理"),
    PROCESSING("processing", "处理中"),
    SUSPENDED("suspended", "已挂起"),
    PENDING_VERIFY("pending_verify", "待验收"),
    COMPLETED("completed", "已完成"),
    CLOSED("closed", "已关闭"),

    PENDING_TEST_ACCEPT("pending_test_accept", "待测试受理"),
    TESTING("testing", "测试中"),
    PENDING_DEV_ACCEPT("pending_dev_accept", "待开发受理"),
    DEVELOPING("developing", "开发中"),
    PENDING_CS_CONFIRM("pending_cs_confirm", "待客服确认");

    private final String code;
    private final String label;

    public static TicketStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TicketStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
