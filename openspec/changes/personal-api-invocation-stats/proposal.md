# 变更提案：个人 API 密钥调用次数统计

## 背景

工单系统已支持通过 `X-Api-Key` 使用**个人 API 密钥**调用业务接口（`AgentApiKeyAuthenticationFilter` 注入与 JWT 等价的身份）。当前仅异步更新 `last_used_at`，用户无法在管理页判断某条密钥的**使用强度**，也不利于安全审计（异常高频调用难以从 UI 侧感知）。

## 现状问题

1. `sys_user_api_key` 无累计调用次数字段；`touchLastUsed` 只写 `last_used_at`。
2. 「个人 API 密钥」列表（`UserApiKeyListOutput` / `UserApiKeysView.vue`）不展示调用量，用户只能依赖最后使用时间推断。

## 目标

- 在**每次**通过个人 API 密钥完成认证并成功建立 `SecurityContext` 后，对该 `keyId` **累计 1 次**调用计数（与现有异步 `touchLastUsed` 同路径，不阻塞主线程）。
- 密钥列表接口返回**累计调用次数**，前端表格增加一列展示。
- 计数为**近似最终一致**（异步写入 acceptable），以数据库为权威来源。

## 非目标

- 不按接口路径、HTTP 方法或响应状态拆分统计（全量为「该密钥下成功鉴权次数」）。
- 不提供运营级报表、导出或按日聚合（可后续独立变更）。
- 不在本变更中引入 Redis 等外部计数中间件（单条 `UPDATE ... SET cnt = cnt + 1` 可满足当前体量）。

## 验收标准

1. 使用有效 `X-Api-Key` 调用任意需鉴权接口后，对应密钥行的**累计次数**在合理延迟后增加（与异步任务一致）。
2. 禁用 / 删除密钥后行为符合预期：禁用仍可读列表并看到历史计数；删除后记录消失。
3. 新建密钥初始计数为 **0**；列表与详情展示一致（仅列表即可）。
4. 前端「个人 API 密钥」页新增「调用次数」列，与后端字段对齐。
5. 后端编译通过；相关 Flyway 迁移可空库/已有库升级执行成功。

## 影响范围

- **数据库**：`sys_user_api_key` 新增列（Flyway 新版本脚本）。
- **后端**：`ticket-infrastructure`（PO/Mapper 若需）、`ticket-application`（`UserApiKeyApplicationService` 异步更新逻辑）、`ticket-entity`（`UserApiKeyListOutput`）。
- **前端**：`miduo-frontend` 中 `userApiKey` 类型与 `UserApiKeysView.vue` 表格列。
- **文档（实施阶段）**：`miduo-md/workflow/工单系统/Agent与IDE集成说明.md` 可补充「列表展示调用次数」一句；若项目要求同步接口编号表且仅扩展响应字段，按仓库惯例自检是否需登记。
