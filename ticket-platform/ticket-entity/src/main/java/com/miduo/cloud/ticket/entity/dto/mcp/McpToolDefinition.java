package com.miduo.cloud.ticket.entity.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具定义，用于 {@code tools/list} 返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpToolDefinition {

    private String name;

    private String description;

    /** JSON Schema（Map 结构），描述工具入参 */
    private Object inputSchema;
}
