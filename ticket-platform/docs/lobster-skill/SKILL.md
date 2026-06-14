---
name: miduo-ticket-lobster-skill
description: 使用个人 API 密钥访问米多内部工单（列表、详情、评论、流转），用于 IDE 内排查缺陷与同步修复说明。
---

# 米多工单 · IDE/龙虾技能

## 触发词

「我的待办工单」「拉工单详情」「把修复说明同步到工单」「工单流转」

## 前置条件

1. 用户在工单 Web **个人 API 密钥** 页创建密钥，配置到环境变量 `MIDUO_TICKET_API_KEY` 或 `config.json`（见 README）。
2. 所有 HTTP 请求携带头：`X-Api-Key: <密钥>`。
3. `baseUrl` 指向 `/api` 根（与前端 `VITE_API_BASE_URL` 一致）。

## 推荐流程

### 排查缺陷

1. `GET {baseUrl}/ticket/page?view=<我待办视图>&pageNum=1&pageSize=20` 获取待处理列表。
2. `GET {baseUrl}/ticket/detail/{id}` 拉详情（描述、附件元数据、评论）。
3. 结合 **当前仓库代码** 本地检索、复现；勿仅凭工单文本臆测根因。

### 修复后同步

1. 让用户 **确认** 评论草稿（根因 + 修改要点 + 可选 commit/PR 链接）。
2. `POST {baseUrl}/ticket/{id}/comment`，body 与 Web 评论接口一致。
3. 若需关单/流转：`GET {baseUrl}/ticket/{id}/available-actions` 再 `PUT {baseUrl}/ticket/transit/{id}`，备注与评论保持一致或摘要。

## 边界

- 不覆盖用户原始工单描述；优先 **新增评论**。
- 不在对话中复述完整 API Key；日志与截图注意脱敏。
- 权限与浏览器登录用户一致，不可越权操作他人私有数据。

## MCP 接入（WorkBuddy，P0 只读）

除裸 HTTP 外，工单系统现提供 **MCP 端点** `POST /api/mcp`（JSON-RPC 2.0），供 WorkBuddy 等支持 MCP 的客户端原生接入。

- **鉴权**：`Authorization: Bearer <个人API密钥>`（或 `X-Api-Key`）。
- **配置示例**（WorkBuddy → 设置 → 连接器(MCP)）：

```json
{
  "mcpServers": {
    "miduo-ticket": {
      "url": "https://<工单域名>/api/mcp",
      "headers": { "Authorization": "Bearer <你的个人API密钥>" }
    }
  }
}
```

- **只读工具（P0）**：`list_my_tickets`、`get_ticket`、`get_ticket_flow_history`、`query_tickets`、`get_available_actions`。
- **边界**：P0 不含任何写操作（评论/流转/受理/完结/转单将于 P1 开放）。
- 角色化触发词见 `mcp/SKILL-dev.md`（开发）、`mcp/SKILL-test.md`（测试）；工具清单见 `mcp/manifest.json`。

## 相关文档

- `miduo-md/workflow/工单系统/Agent与IDE集成说明.md`
- `miduo-md/workflow/工单系统/WorkBuddy-MCP接入说明.md`
- 后端 OpenAPI：`/swagger-ui.html`
