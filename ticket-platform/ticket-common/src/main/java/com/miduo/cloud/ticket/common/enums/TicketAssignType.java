package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单分派事件中的分派原因码（与 {@code TicketAssignedEvent#assignType} 一致），用于 Webhook 等展示中文含义。
 */
@Getter
@AllArgsConstructor
public enum TicketAssignType {

    CREATE_ASSIGN("CREATE_ASSIGN", "创建时指派"),
    MANUAL_ASSIGN("MANUAL_ASSIGN", "手动指派"),
    TRANSFER_ON_TRANSIT("TRANSFER_ON_TRANSIT", "流转时转派"),
    TRANSFER("TRANSFER", "转派"),
    ACCEPT_CLAIM("ACCEPT_CLAIM", "流转认领");

    private final String code;
    private final String label;

    public static TicketAssignType fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.toUpperCase();
        for (TicketAssignType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
