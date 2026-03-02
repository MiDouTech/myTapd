package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TicketView {

    MY_CREATED("my_created", "我创建的"),
    MY_TODO("my_todo", "我待办的"),
    MY_PARTICIPATED("my_participated", "我参与的"),
    MY_FOLLOWED("my_followed", "我关注的"),
    ALL("all", "所有工单");

    private final String code;
    private final String label;

    public static TicketView fromCode(String code) {
        if (code == null) {
            return ALL;
        }
        for (TicketView view : values()) {
            if (view.code.equals(code)) {
                return view;
            }
        }
        return ALL;
    }
}
