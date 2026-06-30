# Task031：业务原生工单插件

> **版本**：v1.0  
> **日期**：2026-06-24  
> **依赖**：Task004、Task010、Task020  
> **对应产品文档**：4.2.1 工单创建、4.11 开放能力  
> **设计方案**：[业务原生工单插件方案.md](./业务原生工单插件方案.md)

---

## 1. 变更背景

产品方案 §4.11 已规划「API + Webhook + 插件」开放能力，当前已实现 JWT/API Key 写接口与 AppKey 只读接口，但 **缺少可嵌入业务系统的 JS SDK 与配套网关**，导致：

- 用户必须跳转工单系统独立域名提单
- 业务系统无法自动携带页面/单据/环境上下文
- AppKey 无法用于插件建单（仅只读）

## 2. 目标与非目标

### 2.1 目标

1. 提供 `ticket-sdk.min.js`，支持 modal / sidebar / float 三种模式
2. 用户 3 步提工单：点入口 → 写描述 → 提交，全程不跳页
3. 接入方 4 步接入：申请应用 → 引入 SDK → setContext → 联调上线
4. 工单详情展示结构化插件上下文，处理人无需反复追问
5. 按应用 Webhook 推送状态变更，业务系统内可查看进度
6. AppKey 写权限 + LaunchToken 双层鉴权，AppSecret 不出前端

### 2.2 非目标

1. 本次不做 iframe 降级（列入 P3）
2. 本次不做录屏采集（仅截图/附件）
3. 本次不做业务系统反向更新工单状态（仅出站 Webhook）
4. 本次不改造现有 `/api/v1/tickets` 接口契约
5. **暂不做管理后台 SDK 离线包下载**（SDK 统一 CDN / npm 分发，管理后台仅提供凭证与配置）

## 3. 交付物清单

| 序号 | 交付物 | 模块 | 阶段 |
|------|--------|------|------|
| 1 | Flyway V12：`integration_app` 表 + `ticket` 扩展字段 | ticket-bootstrap | P0 |
| 2 | 接入应用 CRUD（API000527–530） | ticket-controller | P0 |
| 3 | LaunchToken 签发（API000531） | ticket-application | P0 |
| 4 | 插件建单 API（API000532） | ticket-controller | P0 |
| 5 | 插件查单 API（API000533–534） | ticket-controller | P1 |
| 6 | 富文本图片上传（API000535） | ticket-controller | P1 |
| 7 | 插件配置拉取（API000537） | ticket-controller | P1 |
| 8 | 按应用 Webhook 扩展 | ticket-application | P2 |
| 9 | 工单详情「插件上下文」面板 | miduo-frontend | P2 |
| 10 | 管理端「接入应用」配置页 | miduo-frontend | P1 |
| 11 | `@miduo/ticket-sdk` 包 | ticket-sdk（新目录） | P1 |
| 12 | CDN 发布 `ticket-sdk.min.js` | 运维 | P1 |

## 4. 子任务拆解

### 4.1 P0 — 后端基础（可先 curl 验收）

#### 4.1.1 数据库迁移

- [ ] `V12__integration_app_and_plugin_context.sql`
- [ ] `integration_app` 表（见方案 §8.1）
- [ ] `ticket` 表增加 `integration_app_id`, `external_user_id`, `external_ticket_ref`, `plugin_context`
- [ ] `TicketSourceEnum` 增加 `PLUGIN("plugin")`

#### 4.1.2 接入应用管理

- [ ] `IntegrationAppPO` / `IntegrationAppMapper`
- [ ] `IntegrationAppApplicationService`（CRUD + 密钥轮换 + 加密存储）
- [ ] `IntegrationAppController`（API000527–530）
- [ ] 迁移现有 YAML `open-api.clients` 配置到 DB（兼容期双读）

#### 4.1.3 LaunchToken

- [ ] `PluginLaunchTokenService`：签发/校验 JWT（5 分钟、单次使用、绑定 appKey + externalUserId）
- [ ] `PluginLaunchTokenController`（API000531）
- [ ] 用户映射：`externalUserId` → 查找或创建 `sys_user`（`source=plugin_shadow`）

#### 4.1.4 插件建单

- [ ] `PluginTicketCreateInput` DTO
- [ ] `PluginTicketApplicationService.createTicket()`：
  - 校验 LaunchToken
  - 幂等检查 `external_ticket_ref`
  - 按应用配置解析 `categoryId`（默认 or bizType 映射）
  - 自动生成 title
  - 写入 `plugin_context` + `customFields` 标准键
  - 调用已有 `TicketApplicationService.createTicket()`
- [ ] `PluginTicketController`（API000532）
- [ ] `OpenApiAppAuthFilter` 扩展：路径 `/api/open/v1/plugin/**` 权限校验

**P0 验收**：

```bash
# 1. 管理端创建接入应用，拿到 appKey/appSecret
# 2. 服务端签发 launchToken
# 3. curl 建单成功，工单 source=plugin，plugin_context 完整
```

### 4.2 P1 — SDK + 管理端

#### 4.2.1 ticket-sdk 包

- [ ] 初始化 `ticket-platform/ticket-sdk/` 或独立 repo `miduo-ticket-sdk`
- [ ] 实现 `init / setContext / open / destroy`
- [ ] modal + float 两种 UI
- [ ] 环境信息采集
- [ ] 调用 API000532 建单
- [ ] 构建 UMD `ticket-sdk.min.js`（目标 < 30KB gzip）
- [ ] 编写接入文档 + 星球试点示例页

#### 4.2.2 插件配置与查单 API

- [ ] API000537 插件配置拉取
- [ ] API000533 我的工单列表
- [ ] API000534 工单详情摘要
- [ ] API000535 富文本图片上传（复用七牛逻辑）

#### 4.2.3 管理端

- [ ] `IntegrationAppListView.vue` — 接入应用列表
- [ ] `IntegrationAppFormDialog.vue` — 新建/编辑
- [ ] 联调工具：签发测试 Token + 模拟建单
- [ ] `src/api/integration.ts` 封装

**P1 验收**：

- [ ] 星球测试页引入 SDK，悬浮球提单成功
- [ ] 管理端可 CRUD 接入应用

### 4.3 P2 — 闭环

#### 4.3.1 按应用 Webhook

- [ ] `WebhookDispatchService` 扩展：有 `integration_app_id` 时额外推送 `callback_url`
- [ ] HMAC 签名：`X-Webhook-Signature`
- [ ] payload 增加 `pluginContext` 摘要 + `externalTicketRef`

#### 4.3.2 工单详情上下文面板

- [ ] `TicketDetailView.vue` 增加 `PluginContextPanel.vue`
- [ ] 仅 `source=plugin` 时展示
- [ ] 支持跳转 `pageUrl`、复制 `bizId`

#### 4.3.3 SDK 我的工单

- [ ] `TicketSDK.openMyTickets()` 侧边栏列表
- [ ] `TicketSDK.on('ticket:updated')` 事件

**P2 验收**：

- [ ] 工单状态变更后 30 秒内业务系统收到 Webhook
- [ ] 处理人打开工单可见完整上下文面板

### 4.4 P3 — 体验打磨（可选）

- [ ] sidebar 模式
- [ ] html2canvas 一键截图
- [ ] npm `@miduo/ticket-sdk` 发布
- [ ] iframe 降级方案
- [ ] 多应用推广（BDE 等）

## 5. 接口编号分配

| 接口编号 | 接口名称 | 方法 | 路径 |
|---------|---------|------|------|
| API000527 | 接入应用分页列表 | GET | `/api/integration/app/page` |
| API000528 | 创建接入应用 | POST | `/api/integration/app/create` |
| API000529 | 更新接入应用 | PUT | `/api/integration/app/update/{id}` |
| API000530 | 轮换 AppSecret | POST | `/api/integration/app/rotate-secret/{id}` |
| API000531 | 签发 LaunchToken | POST | `/api/open/v1/plugin/launch-token` |
| API000532 | 插件创建工单 | POST | `/api/open/v1/plugin/tickets` |
| API000533 | 插件我的工单列表 | GET | `/api/open/v1/plugin/tickets/mine` |
| API000534 | 插件工单详情摘要 | GET | `/api/open/v1/plugin/tickets/{ticketNo}` |
| API000535 | 插件富文本图片上传 | POST | `/api/open/v1/plugin/attachments/image` |
| API000537 | 插件初始化配置 | GET | `/api/open/v1/plugin/config` |

## 6. 测试计划

| 场景 | 步骤 | 预期 |
|------|------|------|
| 正常建单 | SDK 填描述提交 | 返回 ticketNo，source=plugin |
| 幂等 | 同一 externalTicketRef 重复提交 | 返回同一 ticketNo |
| 上下文 | 带 bizId/pageUrl 建单 | 详情面板完整展示 |
| 分类映射 | bizType=channel | 进入渠道类工单分类 |
| 鉴权失败 | 过期 launchToken | 401，提示刷新 Token |
| Webhook | 工单状态变更 | 应用 callback_url 收到签名回调 |
| 跨域 | 非白名单域名调 SDK | CORS 拒绝 |

## 7. 回滚策略

1. **功能开关**：`plugin.enabled=false` 关闭插件网关，不影响现有 Web/企微建单
2. **DB 回滚**：新增字段均可空，回滚 migration 不影响存量工单
3. **SDK 回滚**：业务系统移除 `<script>` 引用即可

## 8. 预估工时

| 阶段 | 工时 |
|------|------|
| P0 后端基础 | 4d |
| P1 SDK + 管理端 | 5d |
| P2 闭环 | 3d |
| P3 体验打磨 | 3d |
| **合计** | **15d** |
