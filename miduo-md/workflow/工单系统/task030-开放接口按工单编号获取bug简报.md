# task030-开放接口按工单编号获取Bug简报

## 1. 变更背景

外部系统在联调时，已能通过开放接口拉取工单列表和工单全量数据，但仍缺少“按工单编号直接获取 Bug 简报”的能力。  
对于外部调用方来说，工单号（如 `WO-20260507-004-7459`）是最稳定的业务主键，需要一个一步到位的查询入口。

## 2. 目标与非目标

### 2.1 目标

1. 提供开放接口，支持按 `ticketNo` 查询 Bug 简报摘要。  
2. 返回口径与公开详情页一致：优先返回该工单最新一条“已归档”简报。  
3. 复用现有 `/api/open/v1/**` 鉴权链路（AppKey + 签名），不新增鉴权协议。

### 2.2 非目标

1. 本次不新增 Bug 简报写接口。  
2. 本次不返回全部历史简报，仅返回最新已归档摘要。  
3. 本次不改动 Bug 简报内部审批与通知流程。

## 3. 接口设计

### 3.1 接口清单

| 接口编号 | 接口名称 | 方法 | 路径 |
|---|---|---|---|
| API000514 | 按工单编号查询Bug简报 | GET | `/api/open/v1/bug-report/detail/{ticketNo}` |

### 3.2 出参说明

顶层返回：

- `ticketNo`：工单编号
- `bugReport`：最新归档简报摘要（无归档简报时为 `null`）

`bugReport` 字段包含：

- `id/reportNo/status/statusLabel`
- `defectCategory/severityLevel`
- `logicCauseLevel1/logicCauseLevel2/logicCauseDetail`
- `problemDesc/impactScope/solution/tempSolution`
- `responsibleUserNames/reporterName`
- `introducedProject/startDate/tempResolveDate/resolveDate`
- `reviewedAt/updateTime`

## 4. 错误处理

1. `ticketNo` 为空：返回参数错误。  
2. 工单不存在：返回工单不存在错误码。  
3. 工单存在但无归档简报：返回成功，`bugReport = null`。

## 5. 验收要点

1. 传入有效 `ticketNo`，可返回对应简报摘要。  
2. 传入不存在的 `ticketNo`，返回工单不存在。  
3. 工单有简报但都未归档时，返回 `bugReport = null`。  
4. 接口调用可被开放接口鉴权与操作日志正常记录。
