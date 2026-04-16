package com.miduo.cloud.ticket.application.wecom;

import com.miduo.cloud.ticket.application.wecom.model.WecomBotParseResult;
import com.miduo.cloud.ticket.common.enums.WecomBotCommandType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 企微群机器人消息解析器（通用模板 + 缺陷模板）
 */
@Component
public class WecomBotMessageParser {

    private static final String DEFAULT_PARSE_ERROR =
            "❌ 消息格式无法识别，请按模板发送：\n@工单助手 #分类 标题\n优先级：紧急/高/中/低\n描述：详细描述";

    /**
     * 解析机器人消息
     *
     * @param rawContent          原始消息
     * @param defaultCategoryPath 群默认分类路径（可空）
     */
    public WecomBotParseResult parse(String rawContent, String defaultCategoryPath) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            return WecomBotParseResult.fail(DEFAULT_PARSE_ERROR);
        }

        String content = stripBotMention(rawContent).trim();
        if (content.isEmpty()) {
            return WecomBotParseResult.fail(DEFAULT_PARSE_ERROR);
        }

        if (isHelpCommand(content)) {
            return WecomBotParseResult.success(WecomBotCommandType.HELP);
        }
        if (isCategoryCommand(content)) {
            return WecomBotParseResult.success(WecomBotCommandType.CATEGORY);
        }
        if (content.startsWith("查询")) {
            return parseQueryCommand(content);
        }
        if (content.startsWith("我的工单")) {
            return WecomBotParseResult.success(WecomBotCommandType.MY_TICKETS);
        }
        if (content.startsWith("催办")) {
            return parseUrgeCommand(content);
        }

        if (content.startsWith("#缺陷")) {
            return parseBugTemplate(content);
        }

        if (content.startsWith("#")) {
            return parseGeneralTemplate(content, defaultCategoryPath);
        }

        return WecomBotParseResult.naturalLanguage(content);
    }

    private WecomBotParseResult parseQueryCommand(String content) {
        String ticketNo = content.substring("查询".length()).trim();
        if (ticketNo.isEmpty()) {
            return WecomBotParseResult.fail("❌ 查询指令缺少工单编号，示例：@工单助手 查询 WO-20260228-003");
        }
        WecomBotParseResult result = WecomBotParseResult.success(WecomBotCommandType.QUERY);
        result.setTicketNo(ticketNo);
        return result;
    }

    private WecomBotParseResult parseUrgeCommand(String content) {
        String ticketNo = content.substring("催办".length()).trim();
        if (ticketNo.isEmpty()) {
            return WecomBotParseResult.fail("❌ 催办指令缺少工单编号，示例：@工单助手 催办 WO-20260228-003");
        }
        WecomBotParseResult result = WecomBotParseResult.success(WecomBotCommandType.URGE);
        result.setTicketNo(ticketNo);
        return result;
    }

    private WecomBotParseResult parseGeneralTemplate(String content, String defaultCategoryPath) {
        String[] lines = content.split("\\r?\\n");
        if (lines.length == 0) {
            return WecomBotParseResult.fail(DEFAULT_PARSE_ERROR);
        }

        String firstLine = lines[0].trim();
        String categoryPath;
        String title;

        if (firstLine.startsWith("#")) {
            int blankIndex = firstLine.indexOf(' ');
            if (blankIndex <= 1 || blankIndex >= firstLine.length() - 1) {
                return WecomBotParseResult.fail(DEFAULT_PARSE_ERROR);
            }
            categoryPath = firstLine.substring(1, blankIndex).trim();
            title = firstLine.substring(blankIndex + 1).trim();
        } else {
            if (defaultCategoryPath == null || defaultCategoryPath.trim().isEmpty()) {
                return WecomBotParseResult.fail("❌ 缺少必填信息：工单分类。请使用 #分类 格式指定");
            }
            categoryPath = defaultCategoryPath.trim();
            title = firstLine;
        }

        if (title.isEmpty()) {
            return WecomBotParseResult.fail("❌ 缺少必填信息：工单标题");
        }

        String priority = "medium";
        String description = title;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("优先级")) {
                String priorityText = extractFieldValue(line, "优先级");
                if (!priorityText.isEmpty()) {
                    priority = mapPriority(priorityText);
                }
            } else if (line.startsWith("描述")) {
                StringBuilder descBuilder = new StringBuilder(extractFieldValue(line, "描述"));
                for (int j = i + 1; j < lines.length; j++) {
                    if (descBuilder.length() > 0) {
                        descBuilder.append('\n');
                    }
                    descBuilder.append(lines[j]);
                }
                description = descBuilder.toString().trim();
                break;
            }
        }
        if (description.isEmpty()) {
            description = title;
        }

        WecomBotParseResult result = WecomBotParseResult.success(WecomBotCommandType.CREATE);
        result.setCategoryPath(categoryPath);
        result.setTitle(title);
        result.setDescription(description);
        result.setPriority(priority);
        return result;
    }

    private WecomBotParseResult parseBugTemplate(String content) {
        String[] lines = content.split("\\r?\\n");
        Map<String, String> fieldMap = new LinkedHashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.contains("：") && !trimmed.contains(":")) {
                continue;
            }
            String[] parts = trimmed.split("[：:]", 2);
            if (parts.length == 2) {
                fieldMap.put(parts[0].trim(), parts[1].trim());
            }
        }

        String questionDesc = fieldMap.get("问题描述");
        if (questionDesc == null || questionDesc.trim().isEmpty()) {
            return WecomBotParseResult.fail("❌ 缺陷模板缺少“问题描述”字段");
        }

        String title = questionDesc.length() > 50 ? questionDesc.substring(0, 50) + "..." : questionDesc;
        StringBuilder desc = new StringBuilder();
        for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
            if (desc.length() > 0) {
                desc.append('\n');
            }
            desc.append(entry.getKey()).append("：").append(entry.getValue());
        }

        WecomBotParseResult result = WecomBotParseResult.success(WecomBotCommandType.CREATE);
        result.setCategoryPath("缺陷");
        result.setTitle(title);
        result.setDescription(desc.toString());
        result.setPriority("high");
        result.setCustomFields(fieldMap);
        return result;
    }

    private String stripBotMention(String content) {
        String result = content.trim();
        // 兼容普通空格和不间断空格(\u00a0)；\S* 匹配@工单助手后面可能跟随的任意非空白后缀（如"机器人"）
        result = result.replaceFirst("^@工单助手\\S*[\\s\u00a0]*", "");
        result = result.replaceFirst("^<@[^>]+>[\\s\u00a0]*", "");
        return result;
    }

    private String mapPriority(String priorityText) {
        String normalized = priorityText.trim();
        if ("紧急".equals(normalized) || "urgent".equalsIgnoreCase(normalized)) {
            return "urgent";
        }
        if ("高".equals(normalized) || "high".equalsIgnoreCase(normalized)) {
            return "high";
        }
        if ("低".equals(normalized) || "low".equalsIgnoreCase(normalized)) {
            return "low";
        }
        return "medium";
    }

    private String extractFieldValue(String line, String fieldName) {
        String prefix1 = fieldName + "：";
        String prefix2 = fieldName + ":";
        if (line.startsWith(prefix1)) {
            return line.substring(prefix1.length()).trim();
        }
        if (line.startsWith(prefix2)) {
            return line.substring(prefix2.length()).trim();
        }
        return "";
    }

    private boolean isHelpCommand(String content) {
        return "帮助".equals(content) || "help".equalsIgnoreCase(content);
    }

    private boolean isCategoryCommand(String content) {
        return "分类".equals(content);
    }
}
