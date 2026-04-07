# 实施任务列表：工单评论 @ 提及与企微推送

## task001 — 需求与接口文档对齐

**目标**：在实施前完成 `miduo-md` 与 OpenSpec 变更一致；更新 `功能接口对应关系.md` 中 API000508 的请求体说明（或记录新增用户搜索接口编号）。

**产出**：文档已描述 `mentionedUserIds`、验收标准与企微失败策略。

---

## task002 — 后端：扩展 `TicketCommentInput`

**目标**：为 `TicketCommentInput` 增加 `List<Long> mentionedUserIds`（可选），校验：非 null、单条数量上限、元素非 null。

**文件**：`ticket-entity/.../TicketCommentInput.java`

---

## task003 — 后端：`addComment` 业务与通知

**目标**：在 `TicketApplicationService.addComment` 中：

1. 写入评论后，规范化 `mentionedUserIds`（去重、剔除当前用户、批量查询有效用户）。
2. 对每个目标用户写入站内 `NotificationPO`（类型与催办/评论现有逻辑一致）。
3. 抽取或复用「读取用户通知偏好 + 是否发企微」的私有/包内方法，避免 Controller 写业务。

**注意**：禁止在循环内逐条 `selectById` 用户（遵守 DB 红线），应 `IN` 查询后 `Map` 组装。

---

## task004 — 后端：企微推送异步化

**目标**：对绑定 `wecom_userid` 且开启企微通知的用户调用 `WecomClient.sendTextMessage`；使用 `@Async` 或现有异步基础设施；异常捕获记 warn，不影响评论事务。

**文件**：`TicketApplicationService` 或新建 `TicketCommentNotificationService`（按模块习惯选择，保持 Controller 薄）。

---

## task005 — 前端：评论 @ 选人

**目标**：在 `TicketDetailView` 评论区：

1. 触发 `@` 展示用户建议列表（搜索/分页策略与现有选人组件对齐）。
2. 维护 `mentionedUserIds` 与编辑器内容一致；提交时调用 `addTicketComment`，请求体包含 `content` + `mentionedUserIds`。

**文件**：`TicketDetailView.vue`、`miduo-frontend/src/api/ticket.ts` 及相关类型。

---

## task006 — 联调与回归

**目标**：验证无 @、仅 @、@ 多人、@ 未绑定企微用户、企微 API 失败场景；确认工单列表预览评论区无脚本注入回归（富文本仍走现有 sanitization 策略）。

---

## 执行顺序

task001 → task002 → task003 → task004 → task005 → task006
