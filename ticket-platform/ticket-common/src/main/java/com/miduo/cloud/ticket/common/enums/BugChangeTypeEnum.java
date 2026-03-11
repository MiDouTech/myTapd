package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缺陷变更类型枚举
 * 用于 ticket_log.remark JSON 中标识本次操作的变更来源类型
 */
@Getter
@AllArgsConstructor
public enum BugChangeTypeEnum {

    CREATE("CREATE", "创建缺陷"),
    MANUAL_CHANGE("MANUAL_CHANGE", "手动变更"),
    STATUS_CHANGE("STATUS_CHANGE", "状态流转"),
    SYSTEM_AUTO("SYSTEM_AUTO", "系统自动"),
    COMMENT("COMMENT", "添加评论"),
    ATTACHMENT("ATTACHMENT", "附件操作");

    private final String code;
    private final String label;

    public static BugChangeTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (BugChangeTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
