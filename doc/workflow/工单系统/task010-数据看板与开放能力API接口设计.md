# Task010 数据看板与开放能力 API 接口设计

> **版本**：v1.0
> **日期**：2026-03-02
> **关联任务**：Task010
> **关联产品章节**：4.9 数据看板与报表、4.11 开放能力、5.3 看板视图

---

## 1. 接口清单

| 接口编号 | 接口名称 | 方法 | 路径 | 说明 |
|---|---|---|---|---|
| API000405 | 工单概览仪表盘 | GET | /api/dashboard/overview | 返回待受理、处理中、已挂起、已完成、SLA超时等概览数据 |
| API000406 | 工单趋势统计 | GET | /api/dashboard/trend | 返回新建/关闭/积压趋势（支持 days 参数） |
| API000407 | 分类分布统计 | GET | /api/dashboard/category-distribution | 返回各分类工单数量与占比 |
| API000408 | 处理效率统计 | GET | /api/dashboard/efficiency | 返回平均响应时长、平均解决时长、完成率 |
| API000409 | SLA达成统计 | GET | /api/dashboard/sla-achievement | 返回SLA总量、达成量、超时量、达成率 |
| API000410 | 人员工作量TOP | GET | /api/dashboard/workload | 返回处理量 TOPN（默认TOP10） |
| API000411 | 获取看板数据 | GET | /api/ticket/kanban | 按状态分组返回看板列与卡片数据 |
| API000412 | 看板拖拽变更状态 | PUT | /api/ticket/kanban/move | 拖拽卡片变更目标状态 |
| API000413 | 开放API-创建工单 | POST | /api/v1/tickets | 对外创建工单，来源自动标记为 API |
| API000414 | 开放API-工单详情 | GET | /api/v1/tickets/{id} | 对外查询工单详情 |
| API000415 | 开放API-工单列表 | GET | /api/v1/tickets | 对外分页查询工单列表 |
| API000416 | 开放API-统计概览 | GET | /api/v1/statistics/overview | 对外统计概览接口 |
| API000417 | 分页查询Webhook配置 | GET | /api/webhook/config/page | Webhook配置分页查询 |
| API000418 | 查询Webhook配置详情 | GET | /api/webhook/config/detail/{id} | Webhook配置详情 |
| API000419 | 创建Webhook配置 | POST | /api/webhook/config/create | 创建Webhook配置 |
| API000420 | 更新Webhook配置 | PUT | /api/webhook/config/update/{id} | 更新Webhook配置 |
| API000421 | 删除Webhook配置 | DELETE | /api/webhook/config/delete/{id} | 删除Webhook配置（逻辑删除） |

---

## 2. 核心数据结构

### 2.1 看板拖拽请求（API000412）

```json
{
  "ticketId": 1001,
  "targetStatus": "PROCESSING",
  "remark": "看板拖拽状态变更"
}
```

### 2.2 Webhook配置创建/更新（API000419 / API000420）

```json
{
  "url": "https://example.com/webhook/ticket",
  "secret": "demo-secret",
  "eventTypes": ["TICKET_CREATED", "TICKET_STATUS_CHANGED"],
  "isActive": 1,
  "timeoutMs": 5000,
  "maxRetryTimes": 2,
  "description": "外部系统工单同步"
}
```

### 2.3 Webhook回调事件体（系统推送）

```json
{
  "eventType": "TICKET_STATUS_CHANGED",
  "eventName": "工单状态变更",
  "eventTime": "2026-03-02 16:30:00",
  "ticketId": 1001,
  "ticket": {
    "id": 1001,
    "ticketNo": "WO-20260302-001",
    "title": "支付失败问题",
    "status": "PROCESSING",
    "priority": "high",
    "creatorId": 1,
    "assigneeId": 12
  },
  "data": {
    "oldStatus": "PENDING",
    "newStatus": "PROCESSING",
    "operatorId": 12
  }
}
```

---

## 3. 安全与鉴权

1. `/api/v1/**` 开放接口仍受 JWT 鉴权控制；调用方需携带 `Authorization: Bearer <token>`。
2. Webhook 推送支持在 Header 中透传 `X-Webhook-Secret`（当配置了 secret）。
3. 接口统一返回 `ApiResult<T>` 结构，code=200 表示成功。

---

## 4. 说明

- Task010 已覆盖数据看板、看板拖拽、开放API、Webhook配置管理与事件推送核心能力。
- 报表导出能力由前端报表中心提供 CSV 导出（Excel 可直接打开）。
