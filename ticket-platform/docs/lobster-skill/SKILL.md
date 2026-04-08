---
name: miduo-ticket-lobster-skill
description: 使用个人 API 密钥访问米多内部工单（列表、详情、评论、流转），用于 IDE 内排查缺陷与同步修复说明。
---

# 米多工单 · IDE/龙虾技能

## 触发词

「我的待办工单」「拉工单详情」「把修复说明同步到工单」「工单流转」

## 前置条件

1. 用户在工单 Web **个人 API 密钥** 页创建密钥，配置到环境变量 `MIDUO_TICKET_API_KEY` 或 `config.json`（见 README）。
2. 所有 HTTP 请求携带头：`X-Api-Key: <密钥>`。
3. `baseUrl` 指向 `/api` 根（与前端 `VITE_API_BASE_URL` 一致）。

## 推荐流程

### 排查缺陷

1. `GET {baseUrl}/ticket/page?view=<我待办视图>&pageNum=1&pageSize=20` 获取待处理列表。
2. `GET {baseUrl}/ticket/detail/{id}` 拉详情（描述、附件元数据、评论）。
3. 结合 **当前仓库代码** 本地检索、复现；勿仅凭工单文本臆测根因。

### 修复后同步

1. 让用户 **确认** 评论草稿（根因 + 修改要点 + 可选 commit/PR 链接）。
2. `POST {baseUrl}/ticket/{id}/comment`，body 与 Web 评论接口一致。
3. 若需关单/流转：`GET {baseUrl}/ticket/{id}/available-actions` 再 `PUT {baseUrl}/ticket/transit/{id}`，备注与评论保持一致或摘要。

## 边界

- 不覆盖用户原始工单描述；优先 **新增评论**。
- 不在对话中复述完整 API Key；日志与截图注意脱敏。
- 权限与浏览器登录用户一致，不可越权操作他人私有数据。

## 相关文档

- `miduo-md/workflow/工单系统/Agent与IDE集成说明.md`
- 后端 OpenAPI：`/swagger-ui.html`
