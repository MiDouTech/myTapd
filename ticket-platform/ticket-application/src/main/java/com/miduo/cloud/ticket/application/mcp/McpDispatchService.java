package com.miduo.cloud.ticket.application.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.miduo.cloud.ticket.common.constants.McpConstants;
import com.miduo.cloud.ticket.entity.dto.mcp.JsonRpcResponse;
import com.miduo.cloud.ticket.entity.dto.mcp.McpToolResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP JSON-RPC 方法分发：initialize / tools.list / tools.call / ping / notifications。
 */
@Service
public class McpDispatchService {

    @Resource
    private McpToolService mcpToolService;

    /**
     * 分发单条 JSON-RPC 请求。
     *
     * @param request 解析后的 JSON-RPC 请求体
     * @param userId  当前鉴权用户ID（由端点注入，null 表示未鉴权）
     * @return JSON-RPC 响应；通知类方法返回 null（无需响应体）
     */
    public JsonRpcResponse dispatch(JsonNode request, Long userId) {
        Object id = idOf(request);
        if (request == null || !request.hasNonNull("method")) {
            return JsonRpcResponse.fail(id, McpConstants.ERR_INVALID_REQUEST, "无效请求：缺少 method");
        }
        String method = request.get("method").asText();

        // 通知类（notifications/*）无需响应
        if (method.startsWith(McpConstants.NOTIFICATION_PREFIX)) {
            return null;
        }

        switch (method) {
            case McpConstants.METHOD_INITIALIZE:
                return JsonRpcResponse.ok(id, initializeResult(request));
            case McpConstants.METHOD_PING:
                return JsonRpcResponse.ok(id, new LinkedHashMap<>());
            case McpConstants.METHOD_TOOLS_LIST: {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("tools", mcpToolService.listTools());
                return JsonRpcResponse.ok(id, result);
            }
            case McpConstants.METHOD_TOOLS_CALL:
                return handleToolsCall(id, request, userId);
            default:
                return JsonRpcResponse.fail(id, McpConstants.ERR_METHOD_NOT_FOUND, "未知方法: " + method);
        }
    }

    private JsonRpcResponse handleToolsCall(Object id, JsonNode request, Long userId) {
        if (userId == null) {
            return JsonRpcResponse.fail(id, McpConstants.ERR_UNAUTHORIZED, "未鉴权或凭据无效");
        }
        JsonNode params = request.get("params");
        String name = params != null && params.hasNonNull("name") ? params.get("name").asText() : null;
        if (!StringUtils.hasText(name)) {
            return JsonRpcResponse.fail(id, McpConstants.ERR_INVALID_PARAMS, "缺少参数: name");
        }
        JsonNode arguments = params.get("arguments");
        McpToolResult result = mcpToolService.callTool(name, arguments, userId);
        return JsonRpcResponse.ok(id, result);
    }

    private Map<String, Object> initializeResult(JsonNode request) {
        String protocolVersion = McpConstants.PROTOCOL_VERSION;
        JsonNode params = request.get("params");
        if (params != null && params.hasNonNull("protocolVersion")) {
            protocolVersion = params.get("protocolVersion").asText();
        }
        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name", McpConstants.SERVER_NAME);
        serverInfo.put("version", McpConstants.SERVER_VERSION);

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", new LinkedHashMap<>());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", protocolVersion);
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        return result;
    }

    /**
     * 提取请求 id（保留原始 JSON 类型，由 Jackson 原样序列化）；缺失或 null 返回 null。
     */
    private Object idOf(JsonNode request) {
        if (request == null) {
            return null;
        }
        JsonNode idNode = request.get("id");
        if (idNode == null || idNode.isNull()) {
            return null;
        }
        return idNode;
    }
}
