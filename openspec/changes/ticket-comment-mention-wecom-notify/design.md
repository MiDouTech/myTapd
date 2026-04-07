# 设计文档：工单评论 @ 提及与企微推送

## 1. 整体流程

```
前端：富文本 + mentionedUserIds[]
        │
        ▼ POST /api/ticket/{id}/comment { content, mentionedUserIds? }
        │
        ▼ TicketApplicationService.addComment
        │     ├─ 校验工单存在、权限（与现网一致）
        │     ├─ 写入 ticket_comment（content 不变）
        │     ├─ 规范化 mentionedUserIds：去重、剔除当前用户、剔除非法/禁用用户
        │     ├─ [foreach] 站内通知 COMMENT（或新增子类型 MENTION，见决策表）
        │     └─ [foreach] 若 wecom_userid 非空且用户开启企微通知 → WecomClient.sendTextMessage
        │
        └─ 返回 commentId
```

企微发送应 **异步**（`@Async` 或现有消息队列），避免评论接口被企微 API 超时拖慢；失败仅打日志，**不回滚评论**。

## 2. API 与契约

### 2.1 扩展 `TicketCommentInput`

在保留 `content` 的前提下新增可选字段：

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `mentionedUserIds` | `List<Long>` | 可选，默认空；建议上限 20 | 被 @ 的系统用户主键，以服务端校验为准 |

- 接口路径仍为 `POST /api/ticket/{id}/comment`，与 **API000508** 同一端点，属请求体扩展（向后兼容）。
- 实施时更新 Swagger/OpenAPI 注释与 `miduo-md/workflow/功能接口对应关系.md` 中该接口说明。

### 2.2 评论存储

- **本期可不新增表**：通知依据请求中的 `mentionedUserIds` 即时发送即可。
- **可选增强**（若需审计「历史上 @ 了谁」）：新增 `ticket_comment_mention(comment_id, mentioned_user_id)` 或在 `content` 中保留 `data-user-id` 的 HTML（查询成本高，不推荐单独依赖 HTML）。

## 3. 通知与企微内容

### 3.1 站内通知

- 复用 `NotificationType.COMMENT` 或新增 `COMMENT_MENTION`（若需与普评区分筛选）。
- `title` / `content` 建议包含：工单编号或标题摘要、评论人姓名、截取评论纯文本前 N 字。
- `ticket_id` 关联当前工单，便于通知中心跳转。

### 3.2 企微应用消息

- 复用 `WecomClient.sendTextMessage(wecomUserId, text)`（与 `WecomMessageProcessor` 单聊回复路径一致）。
- 文案示例（实现时统一模板常量或模板服务）：

  > 【工单】{标题或#id}：{评论人} 在评论中@了你：{摘要}。请到工单系统查看详情。

- 尊重 `notification_preference` 中 **企微开关**；与现有 SLA/催办等推送规则对齐（读取偏好逻辑复用同一工具方法）。

## 4. 安全与校验

- **权限**：仅能 @ **当前租户/可见范围内**的用户；禁止通过随意 ID 探测用户存在（失败统一静默或统一错误码，按产品定）。
- **速率**：单条评论 `mentionedUserIds` 上限，防止滥用企微 API。
- **自检**：评论人 @ 自己 → 不发送通知。

## 5. 前端交互（对齐语雀）

- 评论区基于现有富文本编辑器扩展：
  - 输入 `@` 弹出成员列表（可搜索），数据来源：已有用户搜索接口（如工单分派/催办处复用）；若无则新增轻量 `GET` 用户模糊查询（需分配新 API 编号）。
  - 选中后在编辑器中插入带 `data-user-id` 的 `span` 或约定标记，同时维护 `mentionedUserIds` 数组与正文同步。
- **组件深度**：遵守前端嵌套红线，@ 下拉建议抽成独立子组件或与编辑器平级组合，避免超过 2 层嵌套。

## 6. 关键决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 是否新接口 | 扩展 API000508 请求体 | 向后兼容，减少前端分叉 |
| 通知类型 | 优先复用 `COMMENT`，必要时拆 `COMMENT_MENTION` | 减少枚举与前端筛选改动 |
| 企微失败策略 | 异步 + 日志，评论成功 | 与主流程解耦 |
| 数据持久化 mentions | 首期仅请求驱动；表结构可选二期 | 快速交付，降低迁移成本 |

## 7. 风险与应对

| 风险 | 应对 |
|------|------|
| 企微 API 频率限制 | 批量 @ 时限流、上限条数、异步串行发送 |
| 用户未绑定 wecom_userid | 跳过企微，仅站内通知 |
| 富文本与 ID 列表不同步 | 以 `mentionedUserIds` 为通知唯一真相；编辑器在提交前合并去重 |

## 8. 文件变更清单（实施阶段）

```
ticket-entity/.../TicketCommentInput.java          [+mentionedUserIds]
ticket-application/.../TicketApplicationService.java [addComment 扩展 + 通知/企微]
ticket-infrastructure/.../WecomClient.java         [必要时抽公共发送方法]
miduo-frontend/.../TicketDetailView.vue          [@ 交互 + 提交列表]
miduo-frontend/.../api/ticket.ts                   [请求体类型]
miduo-md/workflow/功能接口对应关系.md               [更新 API000508 说明]
miduo-md/workflow/工单系统/工单评论@提及与企微推送-优化.md [需求+方案落地]
```
