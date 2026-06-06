package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批操作类型枚举
 * 借鉴米多星球 FlowActionTypeEnum
 */
@Getter
@AllArgsConstructor
public enum ApprovalActionType {

    APPROVE("approve", "同意"),
    REJECT("reject", "驳回"),
    TRANSFER("transfer", "转交");

    private final String code;
    private final String label;

    public static ApprovalActionType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ApprovalActionType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}
