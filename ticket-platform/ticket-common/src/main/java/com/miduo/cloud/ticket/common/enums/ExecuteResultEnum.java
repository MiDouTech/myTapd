package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作执行结果枚举
 * PRD 附录一 执行结果枚举（ExecuteResultEnum）
 */
@Getter
@AllArgsConstructor
public enum ExecuteResultEnum {

    SUCCESS("SUCCESS", "成功"),
    FAILURE("FAILURE", "失败");

    private final String code;
    private final String desc;

    public static ExecuteResultEnum fromCode(String code) {
        if (code == null) {
            return SUCCESS;
        }
        for (ExecuteResultEnum result : values()) {
            if (result.code.equalsIgnoreCase(code)) {
                return result;
            }
        }
        return SUCCESS;
    }
}
