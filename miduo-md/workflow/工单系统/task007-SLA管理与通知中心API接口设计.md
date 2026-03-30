# Task007 - SLA管理与通知中心 API接口设计

> **版本**：v1.0
> **日期**：2026-03-02
> **对应产品文档**：4.7 SLA管理、4.8 通知中心

---

## 一、SLA策略管理

### API000001 - 查询SLA策略列表

- **路径**：`GET /api/sla/policy/list`
- **说明**：查询所有SLA策略，按ID升序排列
- **请求参数**：无
- **响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "紧急SLA策略",
      "priority": "URGENT",
      "priorityLabel": "紧急",
      "responseTime": 15,
      "resolveTime": 120,
      "warningPct": 75,
      "criticalPct": 90,
      "description": "生产系统故障...",
      "isActive": 1,
      "createTime": "2026-03-02 10:00:00",
      "updateTime": "2026-03-02 10:00:00"
    }
  ],
  "timestamp": 1709380000000
}
```

### API000002 - 创建SLA策略

- **路径**：`POST /api/sla/policy/create`
- **说明**：新增SLA策略配置
- **请求体**：

```json
{
  "name": "自定义SLA策略",
  "priority": "HIGH",
  "responseTime": 60,
  "resolveTime": 480,
  "warningPct": 75,
  "criticalPct": 90,
  "description": "自定义策略描述"
}
```

- **字段说明**：
  - `name`：策略名称（必填）
  - `priority`：优先级 URGENT/HIGH/MEDIUM/LOW（必填）
  - `responseTime`：首次响应时限，单位分钟（必填）
  - `resolveTime`：解决时限，单位分钟（必填）
  - `warningPct`：预警百分比阈值，默认75
  - `criticalPct`：告警百分比阈值，默认90

### API000003 - 更新SLA策略

- **路径**：`PUT /api/sla/policy/update`
- **说明**：更新已有SLA策略配置
- **请求体**：

```json
{
  "id": 1,
  "name": "更新后的策略名",
  "responseTime": 30,
  "resolveTime": 240,
  "isActive": 1
}
```

---

## 二、通知中心

### API000004 - 分页查询通知列表

- **路径**：`GET /api/notification/page`
- **说明**：查询当前用户的站内通知列表（分页）
- **请求参数**：
  - `pageNum`：页码，默认1
  - `pageSize`：每页条数，默认20
  - `type`：通知类型（可选）
  - `isRead`：是否已读 0/1（可选）
- **响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 1,
        "ticketId": 100,
        "type": "SLA_WARNING",
        "typeLabel": "SLA预警",
        "channel": "SITE",
        "channelLabel": "站内信",
        "title": "SLA预警 - 工单 #100",
        "content": "工单响应时限已使用75%...",
        "isRead": 0,
        "readAt": null,
        "createTime": "2026-03-02 10:30:00"
      }
    ],
    "total": 50,
    "pageNum": 1,
    "pageSize": 20,
    "totalPages": 3
  },
  "timestamp": 1709380000000
}
```

### API000005 - 标记通知为已读

- **路径**：`PUT /api/notification/read/{id}`
- **说明**：将指定通知标记为已读
- **路径参数**：`id` - 通知ID

### API000006 - 全部标记已读

- **路径**：`PUT /api/notification/read/all`
- **说明**：将当前用户所有未读通知标记为已读

### API000007 - 查询未读通知数量

- **路径**：`GET /api/notification/unread/count`
- **说明**：查询当前用户未读站内通知数量
- **响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "unreadCount": 5
  }
}
```

### API000008 - 获取用户通知偏好

- **路径**：`GET /api/notification/preference`
- **说明**：获取当前用户所有事件类型的通知偏好设置
- **响应格式**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "userId": 1,
      "eventType": "TICKET_CREATED",
      "eventTypeLabel": "工单创建",
      "siteEnabled": 1,
      "wecomEnabled": 1,
      "emailEnabled": 0
    },
    {
      "userId": 1,
      "eventType": "SLA_WARNING",
      "eventTypeLabel": "SLA预警",
      "siteEnabled": 1,
      "wecomEnabled": 1,
      "emailEnabled": 1
    }
  ]
}
```

### API000009 - 更新用户通知偏好

- **路径**：`PUT /api/notification/preference/update`
- **说明**：批量更新当前用户的通知偏好
- **请求体**：

```json
{
  "items": [
    {
      "eventType": "TICKET_CREATED",
      "siteEnabled": 1,
      "wecomEnabled": 0,
      "emailEnabled": 0
    },
    {
      "eventType": "SLA_WARNING",
      "siteEnabled": 1,
      "wecomEnabled": 1,
      "emailEnabled": 1
    }
  ]
}
```

---

## 三、工单催办

### API000029 - 催办工单

- **路径**：`POST /api/ticket/urge/{id}`
- **说明**：工单处于非终态且已有关联处理人时可催办；默认通知当前工单全部关联处理人（含协同），可选追加其他人
- **路径参数**：`id` - 工单ID
- **请求体**（可选，`application/json`）：

```json
{
  "extraNotifyUserIds": [101, 102]
}
```

- **业务规则**：
  - 终态、`pending_assign`、无关联处理人时不允许催办
  - 发布 `TicketUrgedEvent`（携带完整通知人列表）
  - 通过 `NotificationOrchestrator` 向每位接收人发送通知，类型为 `URGE`

---

## 四、WebSocket实时推送

### /ws/notification

- **协议**：WebSocket
- **连接方式**：`ws://host:port/ws/notification?userId={userId}`
- **说明**：客户端建立WebSocket连接后，服务端会在站内通知创建时自动推送消息
- **推送格式**：

```json
{
  "notificationId": 1,
  "type": "SLA_WARNING",
  "title": "SLA预警 - 工单 #100",
  "content": "工单响应时限已使用75%...",
  "timestamp": 1709380000000
}
```

---

## 五、通知类型枚举

| 类型码 | 说明 |
|--------|------|
| TICKET_CREATED | 工单创建 |
| STATUS_CHANGED | 状态变更 |
| ASSIGNED | 工单分派 |
| SLA_WARNING | SLA预警 |
| SLA_BREACHED | SLA超时 |
| COMMENT | 工单评论 |
| URGE | 催办 |
| REPORT_REMIND | 简报提醒 |

## 六、SLA预警级别

| 级别 | 颜色 | 条件 |
|------|------|------|
| GREEN | 绿色 | 剩余时间 > 50% |
| YELLOW | 黄色 | 25% ≤ 剩余时间 ≤ 50% |
| ORANGE | 橙色 | 剩余时间 < 25% |
| RED | 红色 | 已超时 |
