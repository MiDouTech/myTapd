# task029-外部系统工单开放拉取接口

## 1. 变更背景

外部合作业务系统需要按时间范围拉取工单全量数据，用于流程耗时统计、状态同步和业务分析。  
现有开放接口以登录态/JWT为主，缺少“AppKey + AppSecret + 签名”这一类对外系统常见接入模式。

## 2. 目标与非目标

### 2.1 目标

1. 提供可分页的开放查询接口，支持按创建时间/完成时间范围拉取数据。  
2. 返回工单核心字段 + 流程节点耗时字段，满足外部分析需求。  
3. 接口具备基础安全能力：AppKey 鉴权、签名校验、防重放、限流、调用审计。  
4. 响应结构统一为 `ApiResult + PageOutput`。

### 2.2 非目标

1. 本次不新增写接口（仅查询）。  
2. 本次不做 IP 白名单。  
3. 本次不改动内部工单流程逻辑，仅开放查询能力。

## 3. 接口设计

### 3.1 接口清单

| 接口编号 | 接口名称 | 方法 | 路径 |
|---|---|---|---|
| API000513 | 外部系统分页拉取工单全量数据 | GET | `/api/open/v1/ticket-export/page` |

### 3.2 入参（Query）

| 字段 | 必填 | 说明 |
|---|---|---|
| `createTimeStart` | 条件必填（与 `createTimeEnd` 成对） | 创建时间开始，格式 `yyyy-MM-dd HH:mm:ss` |
| `createTimeEnd` | 条件必填（与 `createTimeStart` 成对） | 创建时间结束，格式 `yyyy-MM-dd HH:mm:ss` |
| `completeTimeStart` | 条件必填（与 `completeTimeEnd` 成对） | 完成时间开始，格式 `yyyy-MM-dd HH:mm:ss` |
| `completeTimeEnd` | 条件必填（与 `completeTimeStart` 成对） | 完成时间结束，格式 `yyyy-MM-dd HH:mm:ss` |
| `statuses` | 否 | 工单状态过滤（可多值） |
| `businessTypeId` | 否 | 业务类型ID（映射工单分类ID） |
| `businessTypeName` | 否 | 业务类型名称（映射工单分类名，模糊匹配） |
| `pageNum` | 否 | 页码，默认1 |
| `pageSize` | 否 | 每页条数，默认20，最大100 |

> 约束：创建时间范围与完成时间范围，至少要提供其中一组。

### 3.3 出参（records 字段）

1. 工单核心：`ticketNo/title/status/statusLabel/createTime/completeTime/briefDescription`  
2. 业务类型：`businessTypeId/businessTypeName`  
3. 流程节点列表 `processNodes[]`：  
   - `nodeName/nodeLabel`  
   - `enterTime/leaveTime`  
   - `processDurationSec`  
   - `handlerName`  
   - `remark`

分页元信息：`total/totalPages/pageNum/pageSize`。

## 4. 安全与治理设计

### 4.1 请求头

| 请求头 | 说明 |
|---|---|
| `X-App-Key` | 应用标识 |
| `X-Timestamp` | Unix 秒/毫秒时间戳 |
| `X-Nonce` | 随机串，防重放 |
| `X-Signature` | HMAC-SHA256 签名 |

### 4.2 签名串规则

按以下顺序拼接（换行分隔）：

1. HTTP 方法（大写）  
2. 请求路径（URI）  
3. 规范化 QueryString（key 升序，value 升序）  
4. `X-Timestamp`  
5. `X-Nonce`  
6. `X-App-Key`

然后用 `AppSecret` 做 `HmacSHA256`，输出十六进制字符串，对比 `X-Signature`。

### 4.3 防重放与限流

1. `nonce` 使用 Redis 做去重（默认 300 秒）。  
2. 单 `AppKey` 分钟级限流（默认 120 次/分钟）。  
3. 超过时间偏移窗口（默认 300 秒）拒绝请求。

### 4.4 审计

Controller 接口加 `@OperationLog`，记录调用路径、请求参数、耗时、结果。

## 5. 错误码约定（本次新增）

| 错误码 | 含义 |
|---|---|
| 8001 | 开放接口应用不存在或已禁用 |
| 8002 | 开放接口签名校验失败 |
| 8003 | 开放接口时间戳已过期 |
| 8004 | 开放接口请求已重复（nonce 重放） |
| 8005 | 开放接口调用过于频繁（限流） |

## 6. 配置项

```yaml
open-api:
  enabled: true
  timestamp-skew-seconds: 300
  nonce-expire-seconds: 300
  rate-limit-per-minute: 120
  clients:
    - app-key: ${OPEN_API_APP_KEY:}
      app-secret: ${OPEN_API_APP_SECRET:}
      app-name: ${OPEN_API_APP_NAME:external-partner}
      enabled: ${OPEN_API_APP_ENABLED:false}
```

## 7. 验收要点

1. 使用正确签名可拉取分页数据。  
2. 缺失签名头、签名错误、时间戳过期、nonce 重放、超频调用都能返回对应错误码。  
3. 返回数据包含工单核心 + 节点耗时字段。  
4. 操作日志中可查询到该接口调用记录。
