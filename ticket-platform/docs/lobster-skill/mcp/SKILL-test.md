---
name: miduo-ticket-mcp-test
description: 测试视角，通过 MCP 访问米多工单：查派给我的单、看工单详情与复现步骤、看流转历史。支持查询和追加评论。
---

# 米多工单 · 测试 MCP 技能

## 触发词

「今天派给我的工单」「我名下待处理的缺陷」「看下这单的复现步骤」「这单转给开发了吗」

## 前置条件

1. 在工单 Web **个人 API 密钥** 页创建密钥；在 WorkBuddy 的 MCP 配置中以 `Authorization: Bearer <密钥>` 连接 `/api/mcp`。
2. 工具均以你的身份执行，可见范围与 Web 一致。

## 推荐流程

1. `list_my_tickets`（`view=my_todo`）→ 你名下待处理工单；缺陷可用 `query_tickets` 配合 `status` 过滤。
2. `get_ticket(ticketId)` → 查看描述与复现步骤、当前状态、处理人。
3. `get_ticket_flow_history(ticketId)` → 确认是否已转开发、当前在谁手上。

## 边界

- **写操作边界**：可用 `add_ticket_comment(ticketId, content, mentionedUserIds?)` 追加评论；先核对目标工单和草稿，并取得用户明确提交授权。正文最多20000字符，最多@20人。不要自动重试；转单、流转、受理和完结仍未开放。
- 不臆造工单号/状态/字段；以工具返回为准。
- 不复述完整 API Key；注意脱敏。
