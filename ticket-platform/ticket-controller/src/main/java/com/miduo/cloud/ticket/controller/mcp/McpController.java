package com.miduo.cloud.ticket.controller.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.miduo.cloud.ticket.application.mcp.McpDispatchService;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.mcp.JsonRpcResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 工单 MCP（Model Context Protocol）服务端点。
 *
 * <p>供 WorkBuddy 等支持 MCP 的 AI 客户端连接，以 JSON-RPC 2.0 暴露只读工具（P0）。
 * 鉴权复用个人 API 密钥：{@code Authorization: Bearer <mdt_...>} 或 {@code X-Api-Key}。
 */
@Tag(name = "MCP 服务", description = "WorkBuddy/MCP 客户端接入（JSON-RPC 2.0，P0 只读）")
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    @Resource
    private McpDispatchService mcpDispatchService;

    /**
     * MCP JSON-RPC 入口
     * 接口编号：API000521
     * 产品文档功能：WorkBuddy 结合 P0 - MCP 只读服务（initialize/tools.list/tools.call/ping）
     */
    @Operation(summary = "MCP JSON-RPC 入口", description = "接口编号：API000521。处理 initialize/tools.list/tools.call/ping；通知类方法返回 202")
    @PostMapping
    public ResponseEntity<JsonRpcResponse> handle(@RequestBody JsonNode request) {
        Long userId = SecurityUtil.getCurrentUserId();
        JsonRpcResponse response = mcpDispatchService.dispatch(request, userId);
        if (response == null) {
            // 通知类（notifications/*）无需响应体
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.ok(response);
    }
}
