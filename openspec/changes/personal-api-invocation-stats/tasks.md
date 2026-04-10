# 实施任务列表：个人 API 密钥调用次数统计

## task001 — 数据库迁移

**目标**：为 `sys_user_api_key` 增加 `invocation_count`。

**内容**：

- 新增 Flyway 脚本（版本号接在 `V45` 之后）：`ALTER TABLE sys_user_api_key ADD COLUMN invocation_count bigint NOT NULL DEFAULT 0 COMMENT '...'`。
- 确认本地/CI 迁移顺序与 `AUTO_INCREMENT` 无关表结构无冲突。

**完成判定**：空库与已有 V45 库执行迁移均成功。

---

## task002 — 后端 PO / 列表 DTO / 异步递增

**目标**：持久化字段贯通，并在鉴权成功后递增计数。

**内容**：

- `SysUserApiKeyPO` 增加 `invocationCount` 映射。
- `UserApiKeyListOutput` 增加 `invocationCount`（Swagger 注解与列表 `toListOutput` 赋值）。
- `UserApiKeyApplicationService`：在异步路径中用 **单条 UPDATE** 同时更新 `last_used_at` 与 `invocation_count = invocation_count + 1`（合并或扩展现有 `touchLastUsed`，避免两次异步或两次 SQL 若无必要）。

**完成判定**：单元或手工验证：调用带 Key 的接口后 DB 中对应行计数 +1。

---

## task003 — 前端类型与列表展示

**目标**：用户可在「个人 API 密钥」页看到调用次数。

**内容**：

- `miduo-frontend/src/api/userApiKey.ts` 中列表项类型增加 `invocationCount`。
- `UserApiKeysView.vue` 表格增加列「调用次数」，遵循现有表格样式（表头 `#f5f7fa` 等）。

**完成判定**：`npm run build` 通过；页面展示与后端一致。

---

## task004 — 文档与自检（可选但推荐）

**目标**：集成说明与行为一致。

**内容**：

- 在 `miduo-md/workflow/工单系统/Agent与IDE集成说明.md`（或等价文档）增加一句：密钥列表展示累计调用次数、异步更新。
- 运行 `ticket-platform` `mvn clean install -DskipTests`（或项目约定命令）与前端 `npm run lint` / `build`。

**完成判定**：命令通过；文档与实现一致。
