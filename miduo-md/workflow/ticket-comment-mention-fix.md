# 工单评论 @ 提醒与选人体验

## 背景与问题

- 评论中使用 @ 同事后，被 @ 用户未收到站内/企微通知。
- 选人依赖点击「@同事」弹层，与常见产品在编辑器内输入 `@` 即搜索的体验不一致。

## 根因（通知）

`TicketEventNotificationListener.onTicketCommentMention` 同时使用 `@TransactionalEventListener(AFTER_COMMIT)` 与 `@Async`。Spring 会先以**同步**方式调用监听器方法，此时事务可能尚未提交；与异步组合时易导致事件处理时机不符合预期，通知分发不可靠。

## 方案概要

1. **通知**：移除该监听方法上的 `@Async`，在事务提交后**同步**调用 `NotificationOrchestrator`（方法内无长阻塞）。
2. **兜底解析**：后端除解析 `data-user-id` 外，识别正文中的 `@显示名(数字用户ID)`，防止富文本清洗掉 data 属性时仍丢失被 @ 人。
3. **前端**：在富文本 `change` 时检测光标前未结束的 `@关键词`，弹出浮动选人面板（语雀式）；选中后删除 `@关键词` 再插入带 ID 的提及片段；保留「@同事」入口共用同一面板。

## 验收

- 评论插入 @ 并提交后，被 @ 用户在通知中心收到「评论@提醒」类型通知（偏好未关闭站内渠道时）。
- 在编辑器中输入 `@` 可搜索并选择用户；展示头像、主行「姓名(工号)」、副行工号（无工号时副行显示用户 ID）。
- **企微应用消息**：用户已绑定 `wecom_userid` 且偏好开启企微时收到卡片；「查看详情」跳转 `ticket.spa-detail-base-url` + `/ticket/detail/{id}`（未配置则回退 `ticket.detail-url` + `/{ticketNo}`）。
- **邮件**：配置 `spring.mail.*` 且用户 `sys_user.email` 非空、偏好开启邮件时发送；正文含通知摘要 + 详情链接。
- **Webhook**：订阅事件类型 `TICKET_COMMENT_MENTION` 后推送 JSON 负载（`data` 含 `mentionedUserIds`、`commentAuthorUserId`、`commentPlainSummary`）。若 URL 为企微群机器人（`qyapi.weixin.qq.com/cgi-bin/webhook/send`），正文含评论摘要且 **`mentioned_list` / `mentioned_mobile_list` 仅包含被 @ 用户**（非创建人/处理人全量 @）。

## 非目标

- 「内容」类 @（文档/工单链接）暂不实现，界面仅作占位提示。
