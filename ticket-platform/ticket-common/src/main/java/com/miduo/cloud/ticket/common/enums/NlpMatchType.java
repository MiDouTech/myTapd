package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * NLP关键词匹配类型枚举
 */
@Getter
@AllArgsConstructor
public enum NlpMatchType {

    CATEGORY(1, "分类"),
    PRIORITY(2, "优先级"),
    ENTITY(3, "实体");

    private final int code;
    private final String label;

    public static NlpMatchType fromCode(int code) {
        for (NlpMatchType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
