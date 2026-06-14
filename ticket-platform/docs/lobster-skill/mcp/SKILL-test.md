---
name: miduo-ticket-mcp-test
description: 测试视角，通过 MCP 只读访问米多工单：查派给我的单、看工单详情与复现步骤、看流转历史。P0 只读，不含写操作。
---

# 米多工单 · 测试 MCP 技能（只读）

## 触发词

「今天派给我的工单」「我名下待处理的缺陷」「看下这单的复现步骤」「这单转给开发了吗」

## 前置条件

1. 在工单 Web **个人 API 密钥** 页创建密钥；在 WorkBuddy 的 MCP 配置中以 `Authorization: Bearer <密钥>` 连接 `/api/mcp`。
2. 工具均以你的身份执行，可见范围与 Web 一致。

## 推荐流程（只读）

1. `list_my_tickets`（`view=my_todo`）→ 你名下待处理工单；缺陷可用 `query_tickets` 配合 `status` 过滤。
2. `get_ticket(ticketId)` → 查看描述与复现步骤、当前状态、处理人。
3. `get_ticket_flow_history(ticketId)` → 确认是否已转开发、当前在谁手上。

## 边界

- **P0 为只读版本**：转单（转给开发）、流转、评论等写操作 **暂未开放**（即将在 P1 上线）；如用户要求转单，请告知当前为只读版本，并可先帮其核对复现步骤是否完整。
- 不臆造工单号/状态/字段；以工具返回为准。
- 不复述完整 API Key；注意脱敏。
