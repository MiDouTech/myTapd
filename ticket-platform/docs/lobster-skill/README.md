# 米多工单 · 龙虾 / IDE 技能包

版本与 `manifest.json` 中 `version` 保持一致。

## 安装（Cursor 等）

1. 将本目录复制到本机 skills 目录，或打包为 zip 后按工具说明安装。
2. 在工单 Web：**用户菜单 → 个人 API 密钥**，创建密钥并复制（仅显示一次）。
3. 复制 `config.example.json` 为 `config.json`（勿提交 Git），填入：
   - `baseUrl`：与前端相同的 API 根路径，如 `https://your-host/api`
   - `apiKey`：完整密钥

## 环境变量方式（推荐）

```bash
export MIDUO_TICKET_BASE_URL=https://your-host/api
export MIDUO_TICKET_API_KEY=mdt_xxxx_yyyy
```

## HTTP 约定

所有请求增加头：

```http
X-Api-Key: <完整密钥>
```

与浏览器登录使用的 `Authorization: Bearer <jwt>` **二选一**即可。

## 常用接口（与 Web 一致）

| 说明 | 方法 | 路径 |
|------|------|------|
| 分页列表 | GET | `/ticket/page` |
| 详情 | GET | `/ticket/detail/{id}` |
| 可用流转 | GET | `/ticket/{id}/available-actions` |
| 流转 | PUT | `/ticket/transit/{id}` |
| 评论 | POST | `/ticket/{id}/comment` |
| 流转历史 | GET | `/ticket/{id}/flow-history` |

列表视图 `view` 参数与 Web 一致，例如「我待办的」等对应当前系统 `TicketView` 编码（参见 OpenAPI 或前端 `TicketListView`）。

## 文档

更完整的集成与安全说明见：`miduo-md/workflow/工单系统/Agent与IDE集成说明.md`。
