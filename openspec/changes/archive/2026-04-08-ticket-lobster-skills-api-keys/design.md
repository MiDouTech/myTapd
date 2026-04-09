# 设计文档：龙虾技能包与个人 API 密钥

## 1. 总体架构

```
┌─────────────────────────────────────────────────────────────────┐
│  用户浏览器（JWT / 企微会话）                                        │
│  ── 仅用于：管理密钥、下载技能包、日常工单操作                          │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  工单平台 ticket-platform                                        │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐ │
│  │ 密钥管理 API     │    │ 业务 API（查询/流转/评论/报表…）         │ │
│  │ （须 JWT）       │    │ 鉴权：JWT **或** API Key + scope       │ │
│  └─────────────────┘    └──────────────────────────────────────┘ │
│           │                              │                         │
│           ▼                              ▼                         │
│  存储：api_key 哈希 + 元数据              审计：key_id / user_id     │
└─────────────────────────────────────────────────────────────────┘
                            ▲
                            │ HTTPS + Header: X-Api-Key 或 Authorization
┌───────────────────────────┴─────────────────────────────────────┐
│  龙虾 / MCP / 本地 Agent（技能包：SKILL + 工具定义，无密钥）            │
│  密钥：环境变量或 ~/.config/...（用户自备，不入库、不进 zip）            │
└─────────────────────────────────────────────────────────────────┘
```

## 2. 密钥模型

| 字段（概念） | 说明 |
|-------------|------|
| `id` | 密钥记录主键，审计与吊销用 |
| `userId` | 绑定创建人，权限上限 = 该用户 RBAC |
| `name` | 用户自定义，如「公司 Mac」 |
| `keyPrefix` | 展示用，如 `mdt_abc1...` |
| `secretHash` | 仅存储哈希（如 BCrypt / HMAC-SHA256 带 pepper），**禁止明文** |
| `scopes` | 可选 JSON 列表，如 `ticket:read`, `ticket:transit`, `report:read` |
| `status` | ACTIVE / DISABLED |
| `createdAt`, `lastUsedAt` | 可选 lastUsedAt 需异步更新避免热路径写放大 |

**生成规则**：创建时生成高熵随机串 → 仅一次返回 `plaintext` → 入库 `hash`。格式可与产品统一（如 `mdt_` 前缀 + 随机段），便于日志脱敏。

**验证流程**：请求带 Key → 查库（可先按 prefix 索引）→ 比对哈希 → 加载 `userId` → 注入与 JWT 等价的 **Security Context**（角色、用户 ID）→ 走现有鉴权链。

## 3. 鉴权与路由策略

**方案 A（推荐）**：Spring Security 增加 `ApiKeyAuthenticationFilter`（顺序在 JWT 之前或之后需评估），成功则设置 `Authentication`；失败则继续 JWT 链。

**方案 B**：独立路径前缀 `/api/agent/v1/**` 仅接受 API Key，与 `/api/**` 用户 JWT 分离，减少过滤器耦合。

**决策建议**：若现有 Controller 已统一从 SecurityContext 取用户，优先 **方案 A**，减少重复接口。

**Scope**：首版可将 scope 与 **角色** 或 **固定白名单接口** 映射；未传 scope 时默认「与登录用户一致的最小集合」或「只读 + 流转」等产品裁定值。

## 4. 技能包产物（无密钥）

建议仓库或发布目录结构（实现阶段落地）：

```
miduo-ticket-lobster-skill/
├── README.md              # 安装步骤、获取密钥、环境变量
├── SKILL.md               # 龙虾触发词、边界、与文档索引
├── manifest.json          # 技能名、版本、minApiVersion、工具列表
├── config.example.json    # {"baseUrl":"...", "apiKey":"YOUR_KEY"}
└── docs/
    └── API.md             # 与 OpenAPI 对齐的 Agent 视角说明
```

**版本策略**：`manifest.json` 的 `version` 与 **兼容的后端 API  minor** 对齐；破坏性变更升 major 并保留旧 Key 兼容窗口（可选）。

## 5. 数据存储

- 新表 `user_api_key`（命名以项目规范为准），含上述字段 + 标准审计字段（若表规范要求 `create_by` 等则一并满足）。
- 索引：`(user_id, status)`、`key_prefix` 唯一或联合唯一，避免扫描全表。

## 6. 安全与合规

| 风险 | 缓解 |
|------|------|
| Key 泄露 | 禁用/删除即失效；建议文档提示定期轮换 |
| 日志泄露 | 仅记录 prefix；过滤器在入日志前剥离 Header |
| 越权 | 权限绑定 userId + RBAC；scope 二次收紧 |
| 暴力破解 | 速率限制 + 锁定策略（可按 IP + prefix） |

## 7. 与现有模块的衔接

- **工作流**：流转接口须仍走 `StateMachineWorkflowEngine`，API Key 仅改变身份注入方式，不改变领域规则。
- **企微**：密钥管理仅 Web；报表推送若后续订阅仍可用系统任务 + 用户授权，与个人 Key 分轨。
- **文档**：`miduo-md` 中增加「Agent 集成」条目，与 OpenAPI / 功能接口对应关系可追溯（若项目要求 API 编号，新增管理接口需单独编号）。

## 8. 回滚与兼容

- 功能开关：配置项关闭 API Key 过滤器后，仅 JWT 可用，密钥表数据保留。
- 删除表或关功能前需确认无外部 Agent 依赖（运维沟通）。

## 9. 文件变更清单（预期）

```
ticket-platform/
├── ticket-bootstrap/          # 迁移脚本、可选配置项
├── ticket-entity/             # Key 相关 DTO / 请求响应
├── ticket-infrastructure/     # Mapper / PO
├── ticket-application/        # ApiKeyAppService、验证逻辑
├── ticket-controller/         # 密钥管理 Controller + 鉴权配置
└── ticket-common/             # 常量、枚举（Key 状态、Scope）

miduo-frontend/
└── src/...                    # 密钥管理页面与 API 封装

openspec/changes/ticket-lobster-skills-api-keys/  # 本变更
artifacts/ 或 release/       # 技能包 zip 的构建产物位置（待 task 敲定）
```
