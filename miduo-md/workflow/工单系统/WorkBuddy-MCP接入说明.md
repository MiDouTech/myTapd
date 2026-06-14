# WorkBuddy · 工单 MCP 接入说明（P0 只读）

> **版本**：v0.1（P0）
> **关联变更**：`openspec/changes/ticket-mcp-readonly`
> **配套**：个人 API 密钥（`Authorization: Bearer` / `X-Api-Key`）、技能包目录 `ticket-platform/docs/lobster-skill/mcp/`

本文面向把「米多工单」接入 **WorkBuddy（腾讯云 CodeBuddy）** 的同事。P0 切片仅提供 **只读** 能力（拉待办、看详情、看流转、条件检索、查看可执行动作），**不含写操作**（评论/流转/受理/完结/转单将在 P1 开放）。

---

## 1. 你将得到什么

| 能力 | MCP 工具 | 说明 |
|------|----------|------|
| 我的工单/待办 | `list_my_tickets` | 范围与 Web 视图一致，按你的角色收敛 |
| 工单详情 | `get_ticket` | 标题、状态、处理人、优先级、描述、附件元数据、评论等 |
| 流转历史 | `get_ticket_flow_history` | 每次状态流转/转派/退回记录 |
| 条件检索 | `query_tickets` | 关键词/状态/分类/优先级/时间范围 |
| 可执行动作 | `get_available_actions` | 仅展示，不执行（P0） |

**不能做的事**：P0 不能写（评论/流转/受理/完结/转单）；不能越权——可见范围与你浏览器登录一致。

---

## 2. 第一步：创建个人 API 密钥

1. 浏览器登录 **工单 Web**（与日常办公相同账号）。
2. 右上角用户菜单 → **个人 API 密钥**（路由：`/account/api-keys`）。
3. **新建密钥** → 立即复制完整密钥（**只显示一次**，形如 `mdt_xxxx_yyyy`）。
4. **不要**把密钥贴进群聊、评论、Wiki 或 Git。

---

## 3. 第二步：在 WorkBuddy 配置 MCP 连接

WorkBuddy → **设置 → 连接器（MCP）→ 添加 MCP Server**：

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

- `url` 指向 `/api/mcp`（注意带 `/api`）。
- 鉴权头 `Authorization: Bearer <密钥>`；亦兼容 `X-Api-Key: <密钥>`。

---

## 4. 第三步：一句话验证

配置完成后，在 WorkBuddy 对话框：

- 「列出我待办工单前 10 条，只显示单号、标题、状态。」（触发 `list_my_tickets`）
- 「拉工单详情 ID=12345，总结描述里的复现步骤，不要编造。」（触发 `get_ticket`）
- 「这单流转到哪一步了？」（触发 `get_ticket_flow_history`）

也可先用 curl 自测（终端，密钥用变量引用，勿写死进 Git）：

```bash
curl -sS -X POST "https://<工单域名>/api/mcp" \
  -H "Authorization: Bearer $MIDUO_TICKET_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

期望返回 P0 的 5 个只读工具定义。再试：

```bash
curl -sS -X POST "https://<工单域名>/api/mcp" \
  -H "Authorization: Bearer $MIDUO_TICKET_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"list_my_tickets","arguments":{"view":"my_todo","pageSize":10}}}'
```

---

## 5. 常见失败排查

| 现象 | 可能原因 |
|------|----------|
| 401 / 鉴权失败 | 密钥被禁用/删除、复制缺字符、`url` 少了 `/api`、未带 `Authorization: Bearer` |
| `tools/list` 为空或报错 | 端点未部署到该环境、协议版本不匹配（`initialize` 时回显） |
| 列表为空 | `view` 与系统枚举不一致（用 `my_todo`/`my_created`/`defect` 等）；或你名下确实无单 |
| 要求写操作被拒 | P0 为只读版本，写操作在 P1 |

---

## 6. 协议说明（实现者）

- 端点：`POST /api/mcp`，JSON-RPC 2.0；方法：`initialize` / `notifications/initialized` / `ping` / `tools/list` / `tools/call`。
- 通知类方法（`notifications/*`）返回 HTTP 202 无响应体。
- P0 单次 JSON 响应（不启用 SSE 流式）。
- 工具结果以 `content:[{type:"text", text:<结构化JSON>}]` 返回。

---

## 7. 相关文档

| 文档 | 用途 |
|------|------|
| `Agent与IDE集成说明.md` | 密钥、Header、管理 API、安全 |
| `ticket-platform/docs/lobster-skill/mcp/` | MCP manifest 与开发/测试 SKILL |
| 后端 OpenAPI：`/swagger-ui.html` | 接口契约 |
