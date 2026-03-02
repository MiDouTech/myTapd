package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum UserRole {

    ADMIN("admin", "系统管理员"),
    TICKET_ADMIN("ticket_admin", "工单管理员"),
    HANDLER("handler", "处理人"),
    SUBMITTER("submitter", "提交人"),
    OBSERVER("observer", "观察者"),
    CUSTOMER_SERVICE("customer_service", "客服"),
    TESTER("tester", "测试"),
    DEVELOPER("developer", "开发");

    private final String code;
    private final String label;

    public static UserRole fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }
}
