package com.miduo.cloud.ticket.entity.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * MCP {@code tools/call} 结果。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolResult {

    private List<McpContent> content;

    private Boolean isError;

    public static McpToolResult ofText(String text) {
        McpToolResult result = new McpToolResult();
        result.setContent(Collections.singletonList(McpContent.text(text)));
        result.setIsError(false);
        return result;
    }

    public static McpToolResult ofError(String text) {
        McpToolResult result = new McpToolResult();
        result.setContent(Collections.singletonList(McpContent.text(text)));
        result.setIsError(true);
        return result;
    }
}
