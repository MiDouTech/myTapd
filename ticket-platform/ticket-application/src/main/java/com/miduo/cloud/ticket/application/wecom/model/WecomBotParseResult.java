package com.miduo.cloud.ticket.application.wecom.model;

import com.miduo.cloud.ticket.common.enums.WecomBotCommandType;
import lombok.Data;

import java.util.Map;

/**
 * 企微机器人消息解析结果
 */
@Data
public class WecomBotParseResult {

    private boolean success;

    private WecomBotCommandType commandType;

    private String errorMessage;

    private String categoryPath;

    private String title;

    private String description;

    private String priority;

    private String ticketNo;

    private Map<String, String> customFields;

    public static WecomBotParseResult success(WecomBotCommandType commandType) {
        WecomBotParseResult result = new WecomBotParseResult();
        result.setSuccess(true);
        result.setCommandType(commandType);
        return result;
    }

    public static WecomBotParseResult fail(String message) {
        WecomBotParseResult result = new WecomBotParseResult();
        result.setSuccess(false);
        result.setCommandType(WecomBotCommandType.UNKNOWN);
        result.setErrorMessage(message);
        return result;
    }
}
