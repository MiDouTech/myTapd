# 工单通知按分类路由 Webhook 方案

> 状态：已实现（2026-07-18）  
> 创建日期：2026-07-18  
> 关联需求：企微群机器人通知需按工单分类（技术缺陷 / 监控告警 / 邮件处理等）分流到不同群

---

## 1. 背景与目标

### 1.1 业务诉求

| 工单分类（示例） | 推送目标 |
|------------------|----------|
| 技术缺陷（含子分类：稳定性缺陷、性能缺陷等） | A 群 Webhook |
| 监控告警 | B 群 Webhook |
| 邮件处理 | C 群 Webhook |

要求：**在现有「按事件类型订阅 Webhook」能力之上，增加「按工单分类过滤」**，且**不破坏已有全局 Webhook 配置的行为**。

### 1.2 非目标

- 不改变站内信、企微应用消息（个人）的通知逻辑
- 不改变日报/月报、告警接入、插件回调等独立 Webhook 通道
- 本次不新增对外 REST 接口路径（在现有 Webhook 配置 CRUD 上扩展字段）

---

## 2. 现状分析

### 2.1 两条并行的企微群推送通道

```
工单事件（创建/状态变更/分派/评论@…）
        │
        ├─► WebhookDispatchService（webhook_config 表）
        │     过滤条件：事件类型 event_types
        │     支持企微机器人 URL 自动转 text 消息
        │     ❌ 无分类过滤
        │
        └─► WecomGroupPushService（wecom_group_binding 表）
              过滤条件：来源群 chat_id OR 默认分类 default_category_id
              ❌ 无事件类型勾选；且必须填 chat_id
              ❌ 若全局 Webhook 已订阅同事件，状态类通知会跳过群绑定（去重）
```

### 2.2 为何不能仅靠「企微群绑定」满足需求

| 限制 | 说明 |
|------|------|
| 强制 chat_id | 建单入口为 Web/API 的工单无 `source_chat_id`，只能走「仅分类」匹配，但 UI 仍要求填群 ChatID |
| 无事件类型维度 | 无法做到「A 群只收创建、B 群只收关闭」 |
| 与 Webhook 配置去重 | 同一 URL 在 `webhook_config` 已存在时，群绑定推送会被跳过 |
| 状态变更兜底逻辑 | `hasActiveWecomSubscriberForAny()` 只要**任意**全局企微 Webhook 订阅了状态事件，就跳过**所有**群绑定的状态推送——未区分分类 |

### 2.3 分类数据结构

- 表：`ticket_category`，字段含 `id`、`parent_id`、`level`、`path`（层级路径）
- 子分类（如「稳定性缺陷」）有独立 `category_id`，与父级「技术缺陷」不同
- 测试/生产环境分类**名称可能不同**（功能缺陷 vs 技术缺陷），必须按 **分类 ID** 配置（与日报 `daily_report_stat_category_ids` 口径一致）

---

## 3. 推荐方案：扩展 Webhook 配置的分类范围

### 3.1 核心思路

在 `webhook_config` 表新增可选字段 **`category_ids`**（逗号分隔的分类 ID 列表）：

| category_ids 配置 | 行为 |
|-------------------|------|
| **空 / 未配置** | 匹配**全部分类**（与现网行为完全一致） |
| **非空**（如 `12,34,56`） | 仅当工单的 `category_id` 命中列表时才推送 |

继续保留现有 **`event_types`** 过滤，两个维度取**交集**：

```
是否推送 = 事件类型命中 AND 分类命中 AND 配置已启用
```

### 3.2 子分类策略（可配置）

新增布尔字段 **`include_descendants`**（默认 `0`）：

| 值 | 行为 |
|----|------|
| `0`（默认） | 仅精确匹配 `category_ids` 中的 ID |
| `1` | 同时匹配这些分类的**所有子孙分类**（利用 `ticket_category.path` 前缀判断） |

**示例**：配置「技术缺陷 ID=10 + include_descendants=1」时，「稳定性缺陷」「性能缺陷」子分类工单也会走 A 群。

### 3.3 兼容原逻辑的保证

| 场景 | 兼容策略 |
|------|----------|
| 已有 Webhook 配置无 `category_ids` | 视为全分类，推送行为不变 |
| 已有事件类型订阅 | 不变，仅在原基础上增加分类过滤 |
| URL 去重 | 保持 `pushToWebhookWithDedup` 按 URL 去重 |
| 企微消息格式 | 保持 `isWecomRobotWebhook` 自动转 compact text |
| 群绑定通道 | **不修改** `wecom_group_binding` 表结构；同步修正 `hasActiveWecomSubscriberForAny` 为**按工单分类**判断，避免误跳过其他分类的群绑定推送 |
| 插件 callback_url | 不涉及 |
| 日报/告警 Webhook | 不涉及 |

### 3.4 目标配置示例（对应截图 A/B/C 群）

在 **系统设置 → 企微设置 → Webhook配置** 新增 3 条（事件类型按需勾选，以下示例为全事件）：

| 配置名称 | Webhook URL | category_ids | include_descendants | event_types |
|----------|-------------|--------------|---------------------|-------------|
| 技术缺陷群 | A 群 key | `技术缺陷分类ID` | 1 | 全选或按需 |
| 监控告警群 | B 群 key | `监控告警分类ID` | 0 | 全选或按需 |
| 邮件处理群 | C 群 key | `邮件处理分类ID` | 0 | 全选或按需 |

> **注意**：分类 ID 需在「工单分类管理」或数据库中按环境查询，不要写死名称。

若仍需一个「管理群收全部」：额外建一条 **category_ids 留空** 的配置即可（与分类专属配置可并存，同一事件会推多个群）。

---

## 4. 技术设计

### 4.1 数据库变更

```sql
-- Vxx__webhook_config_add_category_scope.sql
ALTER TABLE `webhook_config`
  ADD COLUMN `category_ids` varchar(500) DEFAULT NULL COMMENT '适用分类ID（逗号分隔，空=全部分类）' AFTER `event_types`,
  ADD COLUMN `include_descendants` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否包含子分类（0:否 1:是）' AFTER `category_ids`;
```

### 4.2 后端改动点

| 模块 | 改动 |
|------|------|
| `WebhookConfigPO` / DTO | 增加 `categoryIds`、`includeDescendants` |
| `WebhookConfigApplicationService` | 创建/更新校验 ID 合法性；列表/详情回显 |
| `WebhookDispatchService` | ① `dispatchUnionInternal` 取工单 `category_id` 后过滤配置；② 新增 `matchesCategoryScope(config, categoryId)`；③ `hasActiveWecomSubscriberForAny` 增加 `categoryId` 参数 |
| `TicketEventNotificationListener` | `shouldSkipGroupPushForStatusChange` 传入工单当前 `category_id` |
| `CategoryApplicationService` | 复用或新增「展开子孙分类 ID」工具方法（基于 `path`） |

**分类匹配伪代码**：

```java
boolean matchesCategoryScope(WebhookConfigPO config, Long ticketCategoryId) {
    if (isBlank(config.getCategoryIds())) {
        return true; // 兼容：未配置=全部分类
    }
    if (ticketCategoryId == null) {
        return false;
    }
    Set<Long> allowed = parseIds(config.getCategoryIds());
    if (allowed.contains(ticketCategoryId)) {
        return true;
    }
    if (config.getIncludeDescendants() == 1) {
        TicketCategoryPO cat = categoryMapper.selectById(ticketCategoryId);
        // 判断 cat.path 是否以任一配置分类的 path 为前缀
        return isDescendantOfAny(cat, allowed);
    }
    return false;
}
```

**分发流程（变更后）**：

```
事件触发 → 查启用配置 → 按 event_types 过滤 → 按 category_ids 过滤 → URL 去重推送
```

### 4.3 前端改动点

| 页面 | 改动 |
|------|------|
| `WebhookConfigPanel.vue` | 表单增加「适用分类」多选（树形/下拉，数据来自分类树 API）；「包含子分类」开关；列表列展示分类范围 |
| `WebhookDispatchLogPanel.vue` | 可选：日志增加 `category_id` 筛选（排障用） |
| `types/webhook.ts` | 扩展类型定义 |

### 4.4 接口契约扩展（无新路径）

在现有接口请求/响应体上扩展字段（API000417–421 不变）：

```json
{
  "name": "技术缺陷群",
  "url": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=***",
  "eventTypes": ["TICKET_CREATED", "TICKET_STATUS_CHANGED"],
  "categoryIds": [10],
  "includeDescendants": 1,
  "isActive": 1
}
```

`categoryIds` 为空数组或省略 → 等同全部分类。

---

## 5. 边界与风险

| 场景 | 处理 |
|------|------|
| 工单未选分类 | 不命中任何「限定分类」的配置；仍命中「全分类」配置 |
| 工单中途改分类 | 以**触发通知时**的 `category_id` 为准 |
| 同一 URL 多条配置 | 按 URL 去重，只推一次（保持现状） |
| 子分类未勾选「包含子分类」 | 只推精确 ID，子分类需单独配置或开启 include_descendants |
| 环境差异 | 测试/生产分别配置分类 ID，不写死名称 |
| 性能 | 配置条数有限（通常 <20），分发时内存过滤即可；子孙判断可缓存分类 path |

### 回滚策略

1. 代码回滚后 `category_ids` 列可保留（忽略即可）
2. 或将所有配置 `category_ids` 置空，恢复全量推送

---

## 6. 验收标准

1. **兼容**：现有 Webhook 配置不填分类时，推送行为与改前一致（回归：创建/状态/分派/评论@/去重）
2. **分流**：技术缺陷工单仅推 A 群、监控告警仅推 B 群、邮件处理仅推 C 群
3. **子分类**：开启 include_descendants 后，稳定性/性能缺陷随技术缺陷走 A 群
4. **交叉**：同一事件类型 + 不同分类 → 各推各群，互不干扰
5. **日志**：Webhook 推送日志可区分「分类不匹配而跳过」与「无订阅」
6. **群绑定**：未配置全局 Webhook 时，企微群绑定通道行为与改前一致

---

## 7. 实施任务拆分

| 序号 | 任务 | 优先级 |
|------|------|--------|
| 1 | 本文档评审通过 | P0 |
| 2 | Flyway 迁移 + PO/DTO 扩展 | P0 |
| 3 | `WebhookDispatchService` 分类过滤 + `hasActiveWecomSubscriberForAny` 改造 | P0 |
| 4 | Webhook 配置 CRUD 校验与 API 扩展 | P0 |
| 5 | 前端 Webhook 配置页分类多选 UI | P0 |
| 6 | 联调：三分类三群 + 子分类 + 全量兜底配置 | P0 |
| 7 | 更新 `功能接口对应关系.md` / task010 API 设计文档说明字段扩展 | P1 |
| 8 | 更新中心 changelog | P1 |

---

## 8. 备选方案（不采纳为主方案的原因）

### 方案 B：仅用企微群绑定

- 需放宽 `chat_id` 必填、改造 OR 匹配逻辑
- 无法按事件类型细分
- 与 Webhook 配置功能重叠，运维要在两处配置

### 方案 C：独立「分类通知路由」表

- 模型更清晰，但多一张表 + 一套 UI，与 Webhook 配置重复，迁移成本高

**结论**：扩展 `webhook_config` 改动最小、与现网配置习惯一致、兼容性最好。
