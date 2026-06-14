# 设计文档：工单 MCP 只读服务（P0）

## 1. 总体架构

```
  开发/测试 ── WorkBuddy(IDE) ──┐
                               │  MCP over HTTP (JSON-RPC 2.0)
                               │  Authorization: Bearer <个人API密钥>
                               ▼
            ┌─────────────────────────────────────────────┐
            │  POST /api/mcp  (新增, 手写最小MCP端点)         │
            │  initialize / tools/list / tools/call / ping  │
            ├─────────────────────────────────────────────┤
            │  鉴权: 复用 validatePlaintextKey()             │
            │        → 注入 SecurityContext(该用户角色)       │
            ├─────────────────────────────────────────────┤
            │  ToolRegistry → 5个只读Tool                    │
            └───────────────────────┬─────────────────────┘
                                    ▼  复用,零改业务规则
        现有查询服务 (TicketApplicationService / TicketWorkflowAppService)
        /ticket/page · /ticket/detail · /flow-history · /available-actions
                                    │
                                    ▼  权限随身份收敛(复用 TicketView)
                                 工单 DB
```

**核心原则**：MCP 端点只做"协议适配 + 身份注入 + 工具分发"，**事实数据全部来自现有 AppService**；LLM（WorkBuddy 侧）只负责理解与表达，不产生事实。

## 2. 为什么手写 MCP 端点（关键决策）

| 候选 | 结论 |
|---|---|
| 官方 MCP Java SDK (`io.modelcontextprotocol`) | ❌ 要求 Java 17，本项目 JDK 8 |
| Spring AI MCP Server Starter | ❌ 要求 Spring Boot 3 / Java 17 |
| **手写最小 JSON-RPC 2.0 端点** | ✅ 仅需实现 4 个方法，JDK 8 + SB2.7 可行，依赖现有 Jackson |

MCP 的传输层本质是 **JSON-RPC 2.0**。Streamable HTTP 允许服务端对简单请求直接返回 `application/json`（无需 SSE 流），只读工具响应体量小，P0 采用**单次 JSON 响应**即可，无需实现 SSE/会话保持。

> 兼容退路：文章已说明"若工单系统尚未暴露 MCP，可临时用 HTTP/OpenAPI 对接"。本切片实现真 MCP，但现有 REST 仍可作为 WorkBuddy 的 OpenAPI 兜底通道。

## 3. MCP 协议方法（最小实现）

`POST /api/mcp`，请求/响应均为 JSON-RPC 2.0。

| method | 行为 |
|---|---|
| `initialize` | 返回 `protocolVersion`、`capabilities:{tools:{}}`、`serverInfo:{name:"miduo-ticket", version}` |
| `notifications/initialized` | 通知，无返回（204/空 result） |
| `ping` | 返回空 `{}` |
| `tools/list` | 返回工具数组（name / description / inputSchema(JSON Schema)） |
| `tools/call` | 入参 `{name, arguments}` → 执行对应 Tool → `{content:[{type:"text", text:<结构化JSON字符串>}], isError:false}` |

未知 method / 工具 → 返回 JSON-RPC error（`-32601` method not found / `-32602` invalid params）。

## 4. 只读工具清单（P0）

所有工具均以"当前调用者身份"执行，返回与 Web 同口径的结构化数据。

| 工具名 | 入参 | 委托的现有能力 | 对应文章场景（只读部分） |
|---|---|---|---|
| `list_my_tickets` | `view`(枚举,默认 my_todo), `pageNum`, `pageSize` | `/api/ticket/page?view=` | 开发"我的待办"、测试"派给我的" |
| `get_ticket` | `ticketId` | `/api/ticket/detail/{id}` | 看工单详情（标题/状态/处理人/优先级/描述/简报关联等） |
| `get_ticket_flow_history` | `ticketId` | `/api/ticket/{id}/flow-history` | 看"最近流转：测试复现完成→转开发" |
| `query_tickets` | `keyword?`, `status?`, `categoryId?`, `priority?`, `createdFrom?`, `createdTo?`, `pageNum`, `pageSize` | `/api/ticket/page`(过滤) | 客服/产品按条件检索（只读） |
| `get_available_actions` | `ticketId` | `/api/ticket/{id}/available-actions` | 告诉 AI "这单可执行哪些动作"（P0 仅展示，不执行） |

**约定**：
- `view` 取值复用现有 `TicketView` 枚举（`my_todo / my_created / defect / alert / all` 等），工具 description 中列出可用值。
- 工具实现一律走现有分页/批量查询，**禁止循环单查**（遵守仓库红线）。
- 返回 JSON 中字段名与现有 DTO 对齐（`TicketPageOutput` / `TicketDetailOutput`），WorkBuddy 直接消费。
- `get_available_actions` 在 P0 **只读返回**可执行动作，不提供执行入口；执行（transit/transfer）在 P1。

### inputSchema 示例（`list_my_tickets`）

```json
{
  "type": "object",
  "properties": {
    "view": {"type":"string","description":"视图: my_todo/my_created/defect/alert/all","default":"my_todo"},
    "pageNum": {"type":"integer","default":1},
    "pageSize": {"type":"integer","default":20,"maximum":100}
  }
}
```

## 5. 鉴权与身份收敛

```
WorkBuddy 请求头: Authorization: Bearer mdt_xxx...   (个人API密钥)
        │
        ▼  MCP 端点入口
解析 Bearer → 复用 userApiKeyApplicationService.validatePlaintextKey(key)
        │  (与现有 AgentApiKeyAuthenticationFilter 同一校验)
        ├─ 失败/禁用 → JSON-RPC error 或 401
        └─ 成功 → 注入 SecurityContext(userId + roleCodes)
                    │
                    ▼  tools/call 执行时, 现有 AppService 从 SecurityContext 取身份
                       → 同一工具, 不同角色看到不同范围 (权限免费收敛)
```

- **决策：P0 凭据 = 个人 API 密钥（经 Bearer 传递）**。理由：① 复用现有密钥体系与校验逻辑，零新建凭据模型；② P0 目标人群是开发/测试，IDE 中配置一条 Bearer 头无障碍；③ MCP 标准头为 `Authorization: Bearer`，与文章配置示例一致。
- 同时保留 `X-Api-Key` 头兼容（已有过滤器）。
- 企微小程序"全员免密 SSO 直通"是后续切片目标，P0 不做（非目标）。
- Security 配置：放行 `/api/mcp` 的标准过滤链拦截，改由 MCP 端点内部完成 Bearer→身份注入（或扩展 `AgentApiKeyAuthenticationFilter` 额外识别 `Authorization: Bearer mdt_` 前缀）。**优先后者**，复用度更高。

## 6. Skill 包（开发/测试，只读）

在 `ticket-platform/docs/lobster-skill/` 基础上补充 MCP 变体：

```
docs/lobster-skill/
├── SKILL.md                 # 既有(裸HTTP) → 增补"MCP 接入"小节
├── mcp/
│   ├── manifest.json        # MCP server 名称、版本、工具列表(只读)
│   ├── SKILL-dev.md         # 开发触发词: "我的待办/拉详情/看流转"
│   └── SKILL-test.md        # 测试触发词: "派给我的单/看复现步骤"
└── config.example.json      # 增加 mcp url + Bearer 占位
```

- SKILL 明确**边界**：P0 只读；如需写操作请提示用户"当前为只读版本"。
- 触发词与"推荐流程"对齐文章对话（开发查待办→拉详情；测试查派单→看复现步骤），但**不含转单/受理/完结**（那些在 P1 提示为"即将上线"）。

## 7. 审计与安全

| 项 | 措施 |
|---|---|
| 凭据脱敏 | 日志仅记录密钥前缀 + keyId；入日志前剥离 `Authorization`/`X-Api-Key` |
| 调用审计 | 记录 keyId、userId、工具名、时间、（可选）入参摘要 |
| 越权 | 身份绑定 userId + 现有 RBAC/视图；工具不接受"指定他人身份"参数 |
| 限流 | 可复用现有密钥维度的粗粒度限流（若已有）；P0 可暂不强制 |
| 只读保证 | `tools/list` 不暴露任何写工具；端点内不调用任何写 AppService |

## 8. 风险与应对

| 风险 | 概率 | 应对 |
|---|---|---|
| WorkBuddy 的 MCP 实现期望 SSE 流式传输 | 中 | P0 先实现单次 JSON 响应；若客户端要求 SSE，补一个 `text/event-stream` 输出（增量任务，不阻塞协议握手） |
| MCP 协议版本协商不一致 | 低 | `initialize` 回显客户端 `protocolVersion`，并声明支持的版本 |
| Bearer 与 JWT 头冲突 | 低 | 约定：`Bearer mdt_` 前缀走 API Key 校验；标准 JWT 仍走 JWT 链；二者前缀可区分 |
| 角色视图口径与 Web 不一致 | 中 | 工具直接复用 Web 同一 AppService 与 `TicketView`，不另写查询 |
| JDK8 无官方 SDK 导致协议细节遗漏 | 中 | 以 MCP 规范 + 真实 WorkBuddy 握手联调校正；端点设计保持可扩展 |

## 9. 文件变更清单（预期）

```
ticket-platform/
├── ticket-controller/ 或 ticket-bootstrap/
│   └── McpController.java               # POST /api/mcp 入口 + JSON-RPC 分发
├── ticket-application/
│   ├── mcp/McpDispatchService.java      # initialize/tools.list/tools.call 分发
│   ├── mcp/McpToolRegistry.java         # 注册5个只读工具 + inputSchema
│   └── mcp/tool/*.java                  # 各工具实现(委托现有查询AppService)
├── ticket-common/
│   └── constants/McpConstants.java      # 协议版本/方法名/错误码常量
└── ticket-bootstrap/
    └── config/.../SecurityConfig + AgentApiKeyAuthenticationFilter
                                         # 放行/api/mcp + 识别 Authorization: Bearer mdt_

ticket-platform/docs/lobster-skill/
├── SKILL.md                            # 增补 MCP 小节
├── mcp/manifest.json | SKILL-dev.md | SKILL-test.md
└── config.example.json                 # 增加 mcp url + Bearer 占位

miduo-md/workflow/工单系统/
├── WorkBuddy-MCP接入说明.md             # 新增使用文档
└── 功能接口对应关系.md                  # 登记 /api/mcp 接口编号

openspec/changes/ticket-mcp-readonly/   # 本变更
```

## 10. 回滚与兼容

- 功能开关：配置项关闭 `/api/mcp`（或下线 Controller）后系统行为完全回到现状，无 schema 变更需回滚。
- 现有 REST / JWT / `X-Api-Key` 行为不变，WorkBuddy 仍可走 OpenAPI 兜底。
