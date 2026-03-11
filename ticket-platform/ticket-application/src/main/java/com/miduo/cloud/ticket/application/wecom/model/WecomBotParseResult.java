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

    /**
     * 自然语言原始文本（仅在 commandType=NATURAL_LANGUAGE 时有值）
     */
    private String rawNaturalLanguageText;

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

    /**
     * 构建自然语言建单解析结果
     */
    public static WecomBotParseResult naturalLanguage(String rawContent) {
        WecomBotParseResult result = new WecomBotParseResult();
        result.setSuccess(true);
        result.setCommandType(WecomBotCommandType.NATURAL_LANGUAGE);
        result.setRawNaturalLanguageText(rawContent);
        return result;
    }
}
