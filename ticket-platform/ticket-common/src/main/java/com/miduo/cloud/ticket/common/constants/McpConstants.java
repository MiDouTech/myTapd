package com.miduo.cloud.ticket.common.constants;

/**
 * MCP（Model Context Protocol）协议相关常量。
 *
 * <p>工单系统以手写最小 JSON-RPC 2.0 端点的方式暴露 MCP 能力（JDK8/SpringBoot2.7 无法引入官方 SDK）。
 */
public final class McpConstants {

    private McpConstants() {
    }

    /** JSON-RPC 协议版本 */
    public static final String JSONRPC_VERSION = "2.0";

    /** MCP 协议版本（默认声明值；initialize 时优先回显客户端协商版本） */
    public static final String PROTOCOL_VERSION = "2024-11-05";

    /** 服务端标识 */
    public static final String SERVER_NAME = "miduo-ticket";

    /** 服务端版本 */
    public static final String SERVER_VERSION = "0.1.0";

    // ---- MCP / JSON-RPC 方法名 ----
    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_INITIALIZED = "notifications/initialized";
    public static final String METHOD_PING = "ping";
    public static final String METHOD_TOOLS_LIST = "tools/list";
    public static final String METHOD_TOOLS_CALL = "tools/call";

    /** 通知类方法前缀（无需返回 result） */
    public static final String NOTIFICATION_PREFIX = "notifications/";

    // ---- JSON-RPC 标准错误码 ----
    public static final int ERR_PARSE = -32700;
    public static final int ERR_INVALID_REQUEST = -32600;
    public static final int ERR_METHOD_NOT_FOUND = -32601;
    public static final int ERR_INVALID_PARAMS = -32602;
    public static final int ERR_INTERNAL = -32603;

    /** 自定义：鉴权失败 */
    public static final int ERR_UNAUTHORIZED = -32001;
}
