# Task008 - 缺陷工单增强与时间追踪 API 接口设计

> **版本**：v1.0  
> **日期**：2026-03-02  
> **对应产品文档**：4.2.3 缺陷工单详情页、4.4.5 缺陷工单工作流、4.4.6 全链路时间追踪

---

## 一、接口总览

| 接口编号 | 接口名称 | HTTP方法 | 接口路径 | 说明 |
|---|---|---|---|---|
| API000020 | 缺陷工单首次阅读追踪 | POST | /api/ticket/{id}/track/read | 记录缺陷工单首次阅读轨迹 |
| API000021 | 更新缺陷工单客服信息 | PUT | /api/ticket/bug/customer-info/{id} | 更新客服信息区字段 |
| API000022 | 更新缺陷工单测试信息 | PUT | /api/ticket/bug/test-info/{id} | 更新测试信息区字段 |
| API000023 | 更新缺陷工单开发信息 | PUT | /api/ticket/bug/dev-info/{id} | 更新开发信息区字段 |
| API000024 | 获取工单时间追踪链 | GET | /api/ticket/{id}/time-track | 查询工单全链路时间事件 |
| API000025 | 获取工单节点耗时统计 | GET | /api/ticket/{id}/node-duration | 查询节点等待/处理/总耗时 |

---

## 二、接口详细设计

### API000020 - 缺陷工单首次阅读追踪

- **路径**：`POST /api/ticket/{id}/track/read`
- **说明**：进入工单详情页时调用，按“工单+节点+用户”去重记录阅读行为；同节点首条阅读记录打标 `isFirstRead=true`。
- **路径参数**：
  - `id`：工单ID
- **响应**：`ApiResult<Void>`

### API000021 - 更新缺陷工单客服信息

- **路径**：`PUT /api/ticket/bug/customer-info/{id}`
- **说明**：更新客服信息区字段（商户编号、公司名称、商户账号、问题描述、预期结果、场景码、问题截图）。
- **请求体**：

```json
{
  "merchantNo": "M10001",
  "companyName": "示例科技有限公司",
  "merchantAccount": "merchant_demo",
  "problemDesc": "点击提交按钮后页面报错",
  "expectedResult": "提交成功并返回列表页",
  "sceneCode": "ORDER_SUBMIT",
  "problemScreenshot": "https://cdn.example.com/bug/1.png"
}
```

### API000022 - 更新缺陷工单测试信息

- **路径**：`PUT /api/ticket/bug/test-info/{id}`
- **说明**：更新测试信息区字段（复现环境、复现步骤、实际结果、影响范围、缺陷等级、所属模块、复现截图、测试备注）。
- **请求体**：

```json
{
  "reproduceEnv": "PRODUCTION",
  "reproduceSteps": "1. 登录系统 2. 提交订单 3. 点击确认",
  "actualResult": "系统返回500错误",
  "impactScope": "PARTIAL",
  "severityLevel": "CRITICAL",
  "moduleName": "订单中心",
  "reproduceScreenshot": "https://cdn.example.com/bug/repro.png",
  "testRemark": "生产环境稳定复现"
}
```

### API000023 - 更新缺陷工单开发信息

- **路径**：`PUT /api/ticket/bug/dev-info/{id}`
- **说明**：更新开发信息区字段（缺陷原因、修复方案、关联分支、影响评估、开发备注）。
- **请求体**：

```json
{
  "rootCause": "空值判断缺失",
  "fixSolution": "补充参数判空并增加异常兜底",
  "gitBranch": "feature/bugfix/order-null-check",
  "impactAssessment": "仅影响订单提交链路",
  "devRemark": "已回归通过，待发布"
}
```

### API000024 - 获取工单时间追踪链

- **路径**：`GET /api/ticket/{id}/time-track`
- **说明**：返回工单时间链（create/assign/read/start_process/transfer/escalate/return/complete）与操作者信息。
- **响应字段**：
  - `ticketId`
  - `tracks[]`：`action`、`actionLabel`、`fromStatus`、`toStatus`、`fromUserName`、`toUserName`、`isFirstRead`、`timestamp`、`remark`

### API000025 - 获取工单节点耗时统计

- **路径**：`GET /api/ticket/{id}/node-duration`
- **说明**：返回节点耗时统计，支持前端展示等待耗时、处理耗时、总耗时。
- **响应字段**：
  - `ticketId`
  - `nodes[]`：`nodeName`、`assigneeName`、`arriveAt`、`firstReadAt`、`startProcessAt`、`leaveAt`、`waitDurationSec`、`processDurationSec`、`totalDurationSec`

---

## 三、权限与状态规则

- **客服信息编辑**：客服/提交人/管理员，且状态在 `PENDING_DISPATCH / PENDING_TEST / PENDING_CS_CONFIRM`（管理员不限状态）  
- **测试信息编辑**：测试/处理人/管理员，且状态在 `PENDING_TEST / TESTING / PENDING_VERIFY`（管理员不限状态）  
- **开发信息编辑**：开发/处理人/管理员，且状态在 `PENDING_DEV / DEVELOPING / PENDING_VERIFY`（管理员不限状态）  
- 最终权限以服务端校验为准，前端仅做交互层约束。
