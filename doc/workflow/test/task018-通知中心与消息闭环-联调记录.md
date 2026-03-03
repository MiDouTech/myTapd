# Task018 通知中心与消息闭环 - 联调记录

## 1. 联调范围

- 前端工程：`miduo-frontend`
- 后端工程：`ticket-platform`
- 对应任务：`doc/workflow/工单系统/task018-通知中心与消息闭环.md`
- 核心目标：
  1. 顶部铃铛未读数闭环（角标 + 抽屉）
  2. 通知中心列表闭环（分页/筛选/已读）
  3. 通知偏好闭环（编辑/校验/恢复默认）
  4. 实时推送闭环（WebSocket + 轮询兜底）

---

## 2. 交付清单

| 序号 | 交付物 | 路径 | 结果 |
|---|---|---|---|
| 1 | 通知中心页面 | `miduo-frontend/src/views/notification/NotificationCenterView.vue` | ✅ 已完成 |
| 2 | 头部铃铛交互闭环 | `miduo-frontend/src/layouts/MainLayout.vue` | ✅ 已完成 |
| 3 | 通知状态管理 | `miduo-frontend/src/stores/notification.ts` | ✅ 已完成 |
| 4 | 实时连接封装 | `miduo-frontend/src/utils/websocket/notification.ts` | ✅ 已完成 |
| 5 | 通知类型扩展 | `miduo-frontend/src/types/notification.ts` | ✅ 已完成 |
| 6 | 路由接入 | `miduo-frontend/src/router/routes.ts` | ✅ 已完成 |
| 7 | 通知时间筛选后端入参 | `ticket-platform/ticket-entity/.../NotificationPageInput.java` | ✅ 已完成 |
| 8 | 通知时间筛选后端查询 | `ticket-platform/ticket-application/.../NotificationApplicationService.java` | ✅ 已完成 |

---

## 3. 能力闭环说明

### 3.1 顶部铃铛（MainLayout）

- 展示未读角标，超过 99 显示 `99+`
- 点击铃铛打开通知抽屉，展示最近通知
- 抽屉内支持：
  - 单条标记已读
  - 全部标记已读
  - 查看并跳转业务页面（工单详情/报表页）
- 页面切换与页面激活（`visibilitychange`）时自动刷新未读数

### 3.2 通知中心页面

- 路由：`/notification`
- 支持分页查询（`/api/notification/page`）
- 支持筛选：
  - 通知类型
  - 已读状态
  - 时间范围（`createTimeStart/createTimeEnd`）
- 支持操作：
  - 单条标记已读
  - 全部标记已读
  - 点击通知跳转业务页面

### 3.3 通知偏好

- 在通知中心页内提供偏好编辑区块
- 支持按事件类型配置渠道开关（站内信/企微应用/邮件）
- 增加保存前校验：每种通知类型至少保留一个渠道
- 支持“恢复默认配置”（站内信=开，企微=开，邮件=关）

### 3.4 实时推送与兜底

- WebSocket 端点：`/ws/notification?userId={id}`
- 能力：连接、心跳、断线重连
- 兜底：连接异常自动启用定时轮询未读数
- 收到实时消息后同步刷新未读数和最近通知

---

## 4. 接口联调清单

| 接口编号 | 方法 | 路径 | 用途 | 联调结果 |
|---|---|---|---|---|
| API000004 | GET | /api/notification/page | 通知中心列表分页 | ✅ 已打通 |
| API000005 | PUT | /api/notification/read/{id} | 单条已读 | ✅ 已打通 |
| API000006 | PUT | /api/notification/read/all | 全部已读 | ✅ 已打通 |
| API000007 | GET | /api/notification/unread/count | 未读角标 | ✅ 已打通 |
| API000008 | GET | /api/notification/preference | 偏好查询 | ✅ 已打通 |
| API000009 | PUT | /api/notification/preference/update | 偏好保存 | ✅ 已打通 |
| - | WebSocket | /ws/notification | 实时推送 | ✅ 已接入 |

---

## 5. 质量校验结果

### 5.1 前端（miduo-frontend）

- `npm run lint`：✅ 通过
- `npm run build`：✅ 通过

### 5.2 后端（ticket-platform）

- `mvn -pl ticket-application,ticket-entity -am -DskipTests compile`：✅ 通过

---

## 6. 风险与后续建议

1. 当前后端 WebSocket 通过 `userId` 查询参数识别用户，建议后续在 Task020/安全治理中补充鉴权校验。  
2. 通知跳转到报表页当前采用 `/report?reportId=xxx` 透传参数，后续可在报表模块补充“按 reportId 定位”能力。  
3. WebSocket 重连次数已限制，超过阈值后将持续轮询兜底，避免阻断消息感知。
