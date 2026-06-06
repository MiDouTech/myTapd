package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批任务状态枚举
 * 借鉴米多星球 FlowTaskStatusEnum，适配工单审批场景
 */
@Getter
@AllArgsConstructor
public enum ApprovalTaskStatus {

    PENDING("pending", "待审批"),
    WAITING("waiting", "等待中（sequential 模式下未轮到）"),
    APPROVED("approved", "已通过"),
    REJECTED("rejected", "已驳回"),
    TRANSFERRED("transferred", "已转交"),
    SKIPPED("skipped", "已跳过（或签被一票通过时其他人的任务）");

    private final String code;
    private final String label;

    public static ApprovalTaskStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ApprovalTaskStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return null;
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == TRANSFERRED || this == SKIPPED;
    }
}
