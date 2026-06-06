package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批模式枚举
 * 借鉴米多星球 FlowApprovalTypeEnum
 * - single      单人审批（只有一个审批人，或多人中任意一人即可）
 * - countersign 会签（所有人必须审批通过才算通过）
 * - orsign      或签（任意一人通过即整节点通过，其余任务自动跳过）
 * - sequential  依次审批（按 sortOrder 顺序，一人完成后下一人才激活）
 */
@Getter
@AllArgsConstructor
public enum ApprovalMode {

    SINGLE("single", "单人审批"),
    COUNTERSIGN("countersign", "会签"),
    ORSIGN("orsign", "或签"),
    SEQUENTIAL("sequential", "依次审批");

    private final String code;
    private final String label;

    public static ApprovalMode fromCode(String code) {
        if (code == null) {
            return SINGLE;
        }
        for (ApprovalMode m : values()) {
            if (m.code.equals(code)) {
                return m;
            }
        }
        return SINGLE;
    }
}
