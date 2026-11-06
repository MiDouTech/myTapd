package com.miduo.cloud.ticket.common.enums;

import com.miduo.cloud.ticket.common.exception.BusinessException;

/** 评论可见域独立于 COMMENT / OPERATION 内容类型。 */
public enum CommentVisibility {
    PUBLIC, INTERNAL;

    public static CommentVisibility fromInput(String value) {
        if (value == null) {
            return INTERNAL;
        }
        for (CommentVisibility visibility : values()) {
            if (visibility.name().equals(value)) {
                return visibility;
            }
        }
        throw BusinessException.of(ErrorCode.PARAM_ERROR, "评论可见域必须为 PUBLIC 或 INTERNAL");
    }
}
