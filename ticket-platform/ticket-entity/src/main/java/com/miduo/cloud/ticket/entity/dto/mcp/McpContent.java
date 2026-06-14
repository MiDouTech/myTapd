package com.miduo.cloud.ticket.entity.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具调用返回的内容块。P0 仅使用 {@code type=text}，text 为结构化 JSON 字符串。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpContent {

    private String type;

    private String text;

    public static McpContent text(String text) {
        return new McpContent("text", text);
    }
}
