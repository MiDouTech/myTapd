# 实施任务列表：工单 MCP 只读服务（P0）

> 约束：JDK 8 + Spring Boot 2.7.18，手写最小 MCP JSON-RPC 端点；只读，不含写操作。

## task001 — MCP 协议常量与错误码

**目标**：定义协议版本、方法名、JSON-RPC 错误码等常量。

**文件**：`ticket-common/.../constants/McpConstants.java`

**要点**：
- 协议版本（与 MCP 规范一致，如 `2024-11-05` / 客户端协商版本）
- 方法名：`initialize`、`notifications/initialized`、`ping`、`tools/list`、`tools/call`
- 错误码：`-32601`(method not found)、`-32602`(invalid params)、`-32603`(internal)、自定义鉴权错误
- serverInfo 名称/版本常量

---

## task002 — Bearer 鉴权（复用个人 API 密钥）

**目标**：MCP 端点支持 `Authorization: Bearer <个人API密钥>`，注入与 Web 一致的 SecurityContext。

**文件**：`ticket-bootstrap/.../config/agent/AgentApiKeyAuthenticationFilter.java`、`SecurityConfig`

**要点**：
- 扩展过滤器：当 `Authorization` 头以 `Bearer mdt_`（API Key 前缀）开头时，提取密钥走 `userApiKeyApplicationService.validatePlaintextKey()`；标准 JWT 仍走 JWT 链。
- 保留现有 `X-Api-Key` 行为不变。
- `SecurityConfig` 放行 `/api/mcp`（由端点/过滤器内部完成身份注入），未携带有效凭据时 `tools/call` 返回鉴权错误。
- 日志脱敏：剥离 `Authorization` / `X-Api-Key`，仅记录前缀。

---

## task003 — JSON-RPC 请求/响应 DTO

**目标**：定义 JSON-RPC 2.0 请求与响应、工具定义结构。

**文件**：`ticket-entity/.../dto/mcp/`（`JsonRpcRequest`、`JsonRpcResponse`、`McpToolDefinition`、`McpToolCallResult` 等）

**要点**：
- 请求：`jsonrpc`、`id`、`method`、`params`
- 响应：`jsonrpc`、`id`、`result` / `error{code,message,data}`
- 工具定义：`name`、`description`、`inputSchema`（Map/JsonNode）
- 工具调用结果：`content:[{type:"text",text}]`、`isError`

---

## task004 — MCP 工具注册表与 5 个只读工具

**目标**：注册并实现 P0 的 5 个只读工具，委托现有查询 AppService。

**文件**：`ticket-application/.../mcp/McpToolRegistry.java`、`ticket-application/.../mcp/tool/*.java`

**工具**：
1. `list_my_tickets(view, pageNum, pageSize)` → 复用 `/ticket/page` 查询服务
2. `get_ticket(ticketId)` → 复用 `/ticket/detail` 查询服务
3. `get_ticket_flow_history(ticketId)` → 复用 flow-history 查询
4. `query_tickets(keyword/status/categoryId/priority/createdFrom/createdTo/page)` → 复用 `/ticket/page` 过滤
5. `get_available_actions(ticketId)` → 复用 available-actions（只读返回）

**要点**：
- 每个工具提供 `inputSchema`（JSON Schema）；`view` 列出 `TicketView` 可用枚举值。
- 一律走批量/分页查询，禁止循环单查（仓库红线）。
- 以 SecurityContext 当前用户身份执行，权限随角色收敛；**不接受指定他人身份的参数**。
- 返回字段与现有 DTO 对齐（`TicketPageOutput` / `TicketDetailOutput`）。

---

## task005 — MCP 分发服务（initialize/tools.list/tools.call/ping）

**目标**：实现 JSON-RPC 方法分发。

**文件**：`ticket-application/.../mcp/McpDispatchService.java`

**要点**：
- `initialize` → 回显/协商 `protocolVersion`，返回 `capabilities:{tools:{}}`、`serverInfo`
- `notifications/initialized` → 无返回
- `ping` → `{}`
- `tools/list` → 从注册表输出工具数组
- `tools/call` → 校验 name/arguments → 执行 → 包装 `content`；未知方法/参数错误返回对应 JSON-RPC error

---

## task006 — MCP 端点 Controller

**目标**：暴露 `POST /api/mcp`，接收 JSON-RPC，调用分发服务。

**文件**：`ticket-controller/.../mcp/McpController.java`

**要点**：
- 接受 `application/json`，返回 JSON-RPC 响应（P0 单次 JSON，不做 SSE）。
- 鉴权失败/无效凭据 → JSON-RPC error 或 401。
- 按规范分配接口编号（注释 + `@ApiOperation`），登记 `功能接口对应关系.md`。
- controller 仅接收请求、调用 application、返回响应（遵守分层规范）。

---

## task007 — 调用审计

**目标**：记录 MCP 工具调用审计（keyId / userId / 工具名 / 时间）。

**文件**：复用现有操作日志/审计能力或在分发服务中埋点。

**要点**：审计不落完整密钥；可记入参摘要（脱敏）。

---

## task008 — Skill 包（开发/测试，只读 MCP 变体）

**目标**：补充 MCP 接入说明与开发/测试触发词。

**文件**：
- `ticket-platform/docs/lobster-skill/SKILL.md`（增补"MCP 接入"小节）
- `ticket-platform/docs/lobster-skill/mcp/manifest.json`（只读工具清单）
- `ticket-platform/docs/lobster-skill/mcp/SKILL-dev.md`、`SKILL-test.md`
- `config.example.json`（增加 mcp url + `Authorization: Bearer` 占位）

**要点**：明确"P0 只读"边界；触发词对齐文章开发/测试的只读对话；写操作标注"即将上线"。

---

## task009 — 接入文档

**目标**：面向使用者的 WorkBuddy MCP 接入说明。

**文件**：`miduo-md/workflow/工单系统/WorkBuddy-MCP接入说明.md`

**内容**：获取个人 API 密钥 → 在 WorkBuddy 配置 `/api/mcp` + Bearer → 一句话验证（拉待办）；常见失败排查（少 `/api`、密钥禁用、视图枚举拼写）。

---

## task010 — 联调与验收

**目标**：用真实 WorkBuddy / 支持 MCP 的客户端完成握手与只读工具联调。

**要点**（对齐 proposal 验收标准）：
1. `initialize` + `tools/list` 返回只读工具集（无写工具）。
2. 开发/测试用各自密钥调用 `list_my_tickets`，范围与 Web 一致且按角色收敛。
3. `get_ticket` / `get_ticket_flow_history` 字段与 Web 详情一致。
4. 禁用密钥调用 → 鉴权失败。
5. 日志无完整密钥。
6. 现有 REST / JWT / `X-Api-Key` 无回归。

---

## 执行顺序

task001 → task002 → task003 → task004 → task005 → task006 → task007 → task008 → task009 → task010
