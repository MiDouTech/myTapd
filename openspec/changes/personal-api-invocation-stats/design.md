# 设计文档：个人 API 密钥调用次数统计

## 1. 统计口径

| 项目 | 说明 |
|------|------|
| 计数时机 | `AgentApiKeyAuthenticationFilter` 中，在 API Key 校验通过、用户有效、`SecurityContext` 已设置之后，与现有 `touchLastUsed(keyId)` **同一调用点**触发 |
| 计数对象 | 密钥主键 `sys_user_api_key.id` |
| 是否含失败鉴权 | **否**（校验失败或未设置认证时不计数） |
| 是否按请求路径拆分 | **否**，一条密钥一个累计值 |

## 2. 数据模型

在表 `sys_user_api_key` 增加：

- `invocation_count`：`bigint NOT NULL DEFAULT 0`，注释建议为「累计鉴权成功次数（异步递增，近似实时）」。

**索引**：主键更新即可，无需为计数单独建索引。

## 3. 写入策略（避免热路径与 N+1）

- 保持 **`@Async`** 与 `touchLastUsed` 同一异步方法内执行，或拆成两个 `@Async` 方法由过滤器连续触发两次异步调用（实现阶段二选一，优先**单次异步内一条 SQL** 同时更新 `last_used_at` 与 `invocation_count`，减少线程开销）。
- 使用 **单条原子更新**，例如：

```sql
UPDATE sys_user_api_key
SET last_used_at = NOW(),
    invocation_count = invocation_count + 1,
    update_time = NOW()
WHERE id = ?
```

- **禁止**先 `SELECT` 再 `UPDATE` 计算次数（避免竞态与多余 IO）。
- 若与现有 `LambdaUpdateWrapper` 风格一致，可用 `setSql("invocation_count = invocation_count + 1")` 等形式表达自增（具体以 MyBatis-Plus 项目用法为准）。

## 4. API 与 DTO

- **列表接口** `GET /api/user/api-key/list`（现有）：扩展响应字段 `invocationCount`（`Long`），与表字段一致。
- **创建 / 禁用 / 删除**：响应不变；新建记录计数为 0。

## 5. 前端

- `UserApiKeyListOutput` 对应 TS 类型增加 `invocationCount?: number`（或与后端一致的必填 number）。
- `UserApiKeysView.vue` 表格增加「调用次数」列，右对齐或居中，数字展示；大数字可用千分位（可选，任务阶段定）。

## 6. 兼容与回滚

- Flyway 仅 `ADD COLUMN` + `DEFAULT 0`，对已有行无破坏。
- 回滚：保留列不删亦可；若需严格回滚可再写 migration `DROP COLUMN`（一般不执行除非紧急）。

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 极高 QPS 下同 key 行锁竞争 | 当前为内部工单 Agent 场景，可接受；后续可拆 Redis INCR + 周期刷盘 |
| 异步延迟导致用户立刻刷新列表未变 | 产品预期「非实时」；`last_used_at` 已有同类语义 |

## 8. 文件变更清单（预期）

```
ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V46__...sql
ticket-platform/ticket-infrastructure/.../SysUserApiKeyPO.java
ticket-platform/ticket-application/.../UserApiKeyApplicationService.java
ticket-platform/ticket-entity/.../UserApiKeyListOutput.java
miduo-frontend/src/api/userApiKey.ts
miduo-frontend/src/views/account/UserApiKeysView.vue
```
