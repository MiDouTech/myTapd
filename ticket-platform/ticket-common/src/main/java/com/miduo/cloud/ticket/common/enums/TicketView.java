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
        String normalized = code.trim();
        if (normalized.isEmpty()) {
            return ALL;
        }
        normalized = normalized.toLowerCase();
        for (TicketView view : values()) {
            if (view.code.equals(normalized)) {
                return view;
            }
        }
        return ALL;
    }
}
