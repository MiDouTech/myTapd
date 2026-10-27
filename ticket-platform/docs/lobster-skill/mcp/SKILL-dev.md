---
name: miduo-ticket-mcp-dev
description: 开发视角，通过 MCP 访问米多工单：查我的待办、拉工单详情、看流转历史。支持查询和追加评论。
---

# 米多工单 · 开发 MCP 技能

## 触发词

「我的待办工单」「我还有哪些工单没处理」「按优先级看我的待办」「拉工单详情」「这单流转到哪一步了」

## 前置条件

1. 在工单 Web **个人 API 密钥** 页创建密钥；在 WorkBuddy 的 MCP 配置中以 `Authorization: Bearer <密钥>` 连接 `/api/mcp`。
2. 工具均以你的身份执行，可见范围与 Web 一致。

## 推荐流程

1. `list_my_tickets`（默认 `view=my_todo`）→ 得到你的待办列表（单号/标题/状态/优先级）。
2. 对关注的单 `get_ticket(ticketId)` → 看描述、处理人、缺陷开发/测试子信息、评论。
3. `get_ticket_flow_history(ticketId)` → 看最近流转（如「测试复现完成→转开发」）。
4. 如需了解"这单能做什么"，`get_available_actions(ticketId)` → **仅展示**可执行动作。

## 边界

- **写操作边界**：可用 `add_ticket_comment(ticketId, content, mentionedUserIds?)` 追加评论；先核对目标工单和草稿，并取得用户明确提交授权。正文最多20000字符，最多@20人。不要自动重试；转单、流转、受理和完结仍未开放。
- 不臆造工单号/状态/字段；所有事实以工具返回为准。
- 不复述完整 API Key；日志/截图注意脱敏。
