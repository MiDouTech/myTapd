# 变更提案：工单 MCP 只读服务（WorkBuddy 结合 P0）

## 背景

公司计划全员推广 WorkBuddy（腾讯云 CodeBuddy）作为统一 AI 工作台，并希望它能直接读取米多内部工单系统的数据，实现"问答式"工单跟进（详见 `workbuddy` 集成场景文档：六角色对话 + 五大自动化）。WorkBuddy 通过 **MCP（Model Context Protocol）** 协议连接外部系统：用户在 WorkBuddy 配置一个 MCP Server 地址与凭据，即可用自然语言让 AI 调用其暴露的"工具（tools）"。

本提案是整体结合方案的 **第一切片（P0）**：只交付 **MCP Server 接入层 + 只读工具 + 开发/测试 Skill**，让 WorkBuddy 当天即可"读"工单（拉待办、看详情、看流转），**不含任何写操作**。写操作、客服建单、群推送、统计补齐、测试组排班等留待后续切片（P1–P4）。

> 关键约束：后端为 **JDK 8 + Spring Boot 2.7.18**，官方 MCP Java SDK / Spring AI 均要求 Java 17，**无法直接引入**。P0 采用**手写最小 MCP JSON-RPC 端点**实现，工具逻辑复用现有工单查询服务。

## 现状问题

1. 工单平台已具备完整的查询/详情/流转历史 REST 接口与个人 API 密钥（`X-Api-Key`），但**没有 MCP 协议端点**，WorkBuddy 无法以"原生 MCP 工具"方式发现与调用工单能力。
2. 现有 `lobster-skill` 面向 Cursor/裸 HTTP，**缺少 MCP 工具清单（manifest / inputSchema）与 MCP 接入说明**，AI 客户端难以稳定、结构化地选对接口。
3. WorkBuddy 集成文档示例（`Authorization: Bearer {{Token}}` 指向 `/api/mcp`）所描述的端点在当前系统中**尚不存在**。

## 目标

- 新增 **MCP Server 端点 `/api/mcp`**（JSON-RPC 2.0 over Streamable HTTP），实现 `initialize` / `tools/list` / `tools/call` / `ping` 等最小协议方法，WorkBuddy 可成功握手并发现工具。
- 暴露 **一组只读工具**（拉本人待办/列表、看工单详情、看流转历史、查询过滤、查看可执行动作），返回结构化 JSON。
- **鉴权复用现有个人 API 密钥**：MCP 端点接受 `Authorization: Bearer <个人API密钥>`（同时保留 `X-Api-Key` 兼容），凭据背后的用户身份与角色决定可见范围，**权限不高于该用户在 Web 的权限**。
- 提供面向 **开发 / 测试** 两类角色的 **MCP Skill 说明与触发词**（在现有 `lobster-skill` 基础上扩展 MCP 变体），覆盖文章中"开发查待办/看详情、测试看派单/看复现步骤"的**只读**部分。
- 关键调用具备**审计与脱敏**：记录 keyId / 工具名 / 时间；日志不落完整密钥。

## 非目标

- **不实现任何写操作工具**（创建、评论、受理、转单、完结、生成简报、催办、群消息发送）——属 P1。
- **不实现企微小程序免密身份直通（SSO 换 token）**——P0 以个人 API 密钥为凭据，全员零配置的 SSO 直通留待后续切片。
- **不新增任何领域功能**：测试组排班、派单准确率、简报归档率口径、客户分级、处理人负荷率、知识库等均**不在本切片**（P2–P4 或排除）。
- 不引入官方 MCP SDK / Spring AI（JDK 版本不兼容），不做 LLM 编排（自然语言理解与文案生成由 WorkBuddy 侧完成）。
- 不改动现有工作流引擎、领域规则或既有 REST 接口行为。

## 验收标准

1. 在 WorkBuddy / 支持 MCP 的客户端中配置 `{"url":"<host>/api/mcp","headers":{"Authorization":"Bearer <个人API密钥>"}}`，`initialize` 成功，`tools/list` 返回本切片定义的**只读工具集**（不含任何写工具）。
2. 调用 `list_my_tickets` 返回的数据范围**与该用户在 Web 对应视图一致**；不同角色（开发 `developer/handler`、测试 `tester`）以各自身份调用，可见范围不同（权限随密钥背后的用户角色收敛）。
3. 调用 `get_ticket`、`get_ticket_flow_history` 返回工单标题/状态/处理人/优先级/流转记录等结构化字段，与 Web 详情一致。
4. 使用 **已禁用/已删除** 的密钥调用 MCP，返回鉴权失败（JSON-RPC 错误或 401），不泄露数据。
5. 服务端日志与审计中**不出现完整密钥**，仅记录前缀与 keyId；记录工具调用审计。
6. 现有 REST 接口、JWT/`X-Api-Key` 行为无回归。

## 影响范围

- **后端**：
  - `ticket-controller`（或 `ticket-bootstrap`）：新增 `/api/mcp` 端点与请求分发。
  - `ticket-application`：MCP 方法分发、工具注册与 `tools/call` 执行（委托现有工单查询 AppService）。
  - `ticket-common`：MCP 协议常量、Bearer 凭据常量、（可选）Scope 枚举。
  - `ticket-bootstrap`：Spring Security 放行 `/api/mcp` 并支持 `Authorization: Bearer <API Key>` 解析（复用 `AgentApiKeyAuthenticationFilter` 校验逻辑）。
- **文档/技能包**：`ticket-platform/docs/lobster-skill/`（新增 MCP manifest 与开发/测试 SKILL 说明）；`miduo-md/workflow/工单系统/` 增补 WorkBuddy MCP 接入说明。
- **接口编号**：新增 MCP 端点按规范分配接口编号并登记到 `功能接口对应关系.md`（实现时取下一个可用号）。
- **不涉及**：数据库 schema（P0 无新表）、前端页面、定时任务。
