package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentType {

    COMMENT("COMMENT", "评论"),
    OPERATION("OPERATION", "操作记录");

    private final String code;
    private final String label;

    public static CommentType fromCode(String code) {
        if (code == null) {
            return COMMENT;
        }
        for (CommentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return COMMENT;
    }
}
