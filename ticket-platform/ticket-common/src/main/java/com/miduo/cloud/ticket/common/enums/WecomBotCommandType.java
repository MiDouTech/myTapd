package com.miduo.cloud.ticket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 企微机器人指令类型
 */
@Getter
@AllArgsConstructor
public enum WecomBotCommandType {

    HELP("HELP", "帮助"),
    CATEGORY("CATEGORY", "分类列表"),
    QUERY("QUERY", "查询工单"),
    MY_TICKETS("MY_TICKETS", "我的工单"),
    URGE("URGE", "催办工单"),
    CREATE("CREATE", "创建工单"),
    UNKNOWN("UNKNOWN", "未知指令");

    private final String code;
    private final String label;
}
