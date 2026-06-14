package com.miduo.cloud.ticket.entity.dto.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.miduo.cloud.ticket.common.constants.McpConstants;
import lombok.Data;

/**
 * JSON-RPC 2.0 响应体。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {

    private String jsonrpc = McpConstants.JSONRPC_VERSION;

    /** 与请求 id 对应；通知（无 id）不返回响应 */
    private Object id;

    private Object result;

    private JsonRpcError error;

    public static JsonRpcResponse ok(Object id, Object result) {
        JsonRpcResponse resp = new JsonRpcResponse();
        resp.setId(id);
        resp.setResult(result);
        return resp;
    }

    public static JsonRpcResponse fail(Object id, int code, String message) {
        JsonRpcResponse resp = new JsonRpcResponse();
        resp.setId(id);
        resp.setError(new JsonRpcError(code, message, null));
        return resp;
    }
}
