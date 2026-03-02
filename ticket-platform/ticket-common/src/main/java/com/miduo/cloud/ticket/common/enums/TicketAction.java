package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketAction {

    CREATE("CREATE", "创建工单"),
    UPDATE("UPDATE", "更新工单"),
    ASSIGN("ASSIGN", "分派工单"),
    TRANSFER("TRANSFER", "转派工单"),
    ACCEPT("ACCEPT", "受理工单"),
    PROCESS("PROCESS", "处理工单"),
    SUSPEND("SUSPEND", "挂起工单"),
    RESUME("RESUME", "恢复工单"),
    VERIFY_PASS("VERIFY_PASS", "验收通过"),
    VERIFY_REJECT("VERIFY_REJECT", "验收不通过"),
    CLOSE("CLOSE", "关闭工单"),
    REOPEN("REOPEN", "重新打开"),
    FOLLOW("FOLLOW", "关注工单"),
    UNFOLLOW("UNFOLLOW", "取消关注"),
    COMMENT("COMMENT", "添加评论"),
    STATUS_CHANGE("STATUS_CHANGE", "状态变更");

    private final String code;
    private final String label;

    public static TicketAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TicketAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }
}
