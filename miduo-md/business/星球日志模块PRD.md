# 星球日志模块 — 产品需求文档（PRD）

> 版本：v1.0  
> 日期：2026-03-16  
> 所属系统：米多内部工单系统（Miduo Ticket Platform）  
> 模块位置：设置 → 日志管理 → 星球日志  
> API 编号段：API000600 – API000699

---

## 一、项目背景与目标

### 1.1 背景

随着工单系统的持续运营，系统内发生的各类操作行为（登录、数据修改、配置变更、权限操作等）需要被完整记录和审计。当前系统缺乏统一的操作日志管理模块，导致：

- **安全审计缺失**：无法追溯历史操作行为，出现安全事件时难以溯源
- **操作记录分散**：各业务模块日志分散，运营人员难以统一查阅
- **故障排查困难**：系统异常时缺乏完整的操作上下文信息
- **合规需求未满足**：企业内控审计要求完整的操作留存记录

### 1.2 目标

构建统一的操作日志管理模块（星球日志），实现：

| 目标 | 描述 |
|------|------|
| 全链路记录 | 覆盖系统内所有关键操作行为，自动写入日志 |
| 多维度检索 | 支持按时间、操作人、模块、级别、结果等多条件检索 |
| 详情可溯源 | 提供操作前后的字段变更详情，支持完整审计 |
| 可视化展示 | 日志级别、执行结果以彩色标签展示，提升可读性 |
| 合规存档 | 日志数据长期保存，支持导出，满足企业审计需求 |

---

## 二、用户角色与使用场景

### 2.1 目标用户

| 角色 | 使用场景 |
|------|----------|
| 系统管理员 | 查询全局操作日志，排查安全异常，导出审计报告 |
| 超级管理员 | 监控关键配置变更，审查敏感操作，做合规留存 |
| 运营人员 | 查阅特定时间段内的操作记录，辅助数据核对 |
| IT 安全人员 | 通过 IP / 账号维度排查异常访问行为 |

### 2.2 核心使用场景

**场景一：安全事件溯源**  
发现数据异常，管理员通过"操作时间 + 操作人IP"定位到具体操作记录，查看详情中的变更内容，确认是否为误操作或恶意行为。

**场景二：权限变更审计**  
定期检查权限管理模块的操作日志，确保权限变更均有记录，并符合审批流程。

**场景三：故障排查协助**  
系统某功能出现异常，研发人员通过查询"操作模块 + 执行结果=失败"，快速定位错误操作上下文和具体失败路径。

**场景四：合规报告导出**  
季度合规检查时，管理员按时间段导出操作日志，生成 Excel 报告提交审计。

---

## 三、功能需求详述

### 3.1 日志列表页

#### 3.1.1 搜索筛选区

提供多维度联合筛选条件，支持任意组合查询：

| 筛选项 | 类型 | 说明 |
|--------|------|------|
| 操作时间 | 日期范围选择器 | 选择操作发生的起止时间，默认展示最近7天数据 |
| 操作账号ID | 文本输入框 | 精确匹配操作者的系统账号ID |
| 操作人 | 文本输入框 | 模糊匹配操作人姓名 |
| 操作人IP | 文本输入框 | 支持精确IP（192.168.1.1）或IP段（192.168.1.）输入 |
| 日志级别 | 下拉选择 | 可选：全部 / 系统级 / 业务级 / 安全级 / 错误级 |
| 所属应用 | 下拉选择 | 从系统注册的应用列表动态获取，如：米多球基础、米多后台等 |
| 操作模块 | 文本输入框 | 模糊匹配操作模块名称，如：登录模块、流程管理等 |
| 操作项 | 文本输入框 | 模糊匹配具体操作名称，如：企业微信登录接收回调、删除表单等 |
| 操作详情 | 文本输入框 | 在操作详情内容中进行关键词模糊搜索 |
| 执行结果 | 下拉选择 | 可选：全部 / 成功 / 失败 |

**操作按钮：**
- **搜索**：触发查询，返回第一页数据
- **重置**：清空所有筛选条件，恢复默认状态

#### 3.1.2 日志数据表格

| 列名 | 字段 | 宽度策略 | 说明 |
|------|------|----------|------|
| 操作时间 | operateTime | 固定160px | 精确到秒，格式 `yyyy-MM-dd HH:mm:ss`，支持升序/降序排序，默认按降序排列 |
| 操作账号ID | accountId | 固定100px | 操作者的系统账号ID（数字） |
| 操作人 | operatorName | 固定80px | 操作者姓名 |
| 操作人IP | operatorIp | 固定130px | 操作发起的客户端IP地址 |
| 日志级别 | logLevel | 固定90px | 以彩色标签展示，见 §3.4 日志级别枚举 |
| 所属应用 | appName | 自适应 | 所属的应用系统名称 |
| 操作模块 | moduleName | 自适应 | 业务模块名称，展示省略号，hover显示全文 |
| 具体路径 | requestPath | 自适应 | 接口请求路径，展示省略号，hover显示全文 |
| 操作项 | operationItem | 自适应 | 具体的操作动作名称，展示省略号，hover显示全文 |
| 执行结果 | executeResult | 固定90px | 以彩色标签展示：成功（绿色）/ 失败（红色） |
| 操作 | - | 固定80px | 固定在右侧，提供「详情」文字按钮 |

**表格规范：**
- 表头浅灰色背景（`#f5f7fa`），居中对齐
- 无边框，带行间分隔线
- 支持"操作时间"列排序
- 分页：每页默认20条，可切换10/20/50/100条，显示总记录数

#### 3.1.3 分页组件

```
共 X 条  [10 ▼]  [上一页] [1][2][3]...[N] [下一页]  跳至 [ ] 页
```

### 3.2 日志详情弹窗/抽屉

点击列表中「详情」按钮，以侧边抽屉（Drawer）形式展开，展示完整日志信息：

#### 3.2.1 基础信息区

| 字段 | 说明 |
|------|------|
| 日志ID | 系统生成的唯一日志编号 |
| 操作时间 | 精确到毫秒的时间戳 |
| 操作账号ID | 操作者账号ID |
| 操作人 | 操作者姓名 |
| 操作人IP | 客户端IP地址 |
| User-Agent | 请求来源的浏览器/客户端信息 |
| 日志级别 | 带颜色标签展示 |
| 所属应用 | 应用名称 |
| 操作模块 | 模块名称 |
| 具体路径 | 完整的接口请求路径（含HTTP方法） |
| 请求方式 | HTTP 请求方式（GET / POST / PUT / DELETE） |
| 操作项 | 具体操作动作描述 |
| 执行结果 | 成功 / 失败，带颜色标签 |
| 耗时 | 接口执行耗时，单位：毫秒（ms） |

#### 3.2.2 请求参数区

以 JSON 格式展示本次操作的完整请求参数，支持格式化高亮显示：

```json
{
  "ticketId": 123,
  "status": "CLOSED",
  "remark": "处理完毕"
}
```

#### 3.2.3 操作内容区（变更记录）

当操作涉及数据变更（新增/编辑/删除）时，展示变更前后的字段对比：

| 字段名 | 变更前 | 变更后 |
|--------|--------|--------|
| 工单状态 | 处理中 | 已关闭 |
| 处理备注 | （空） | 处理完毕 |

#### 3.2.4 错误信息区（仅执行失败时展示）

展示错误码、错误描述和异常堆栈摘要（可展开/收起）：

```
错误码：500
错误信息：工单不存在或已被删除
异常摘要：com.miduo.cloud.exception.BusinessException: 工单不存在...
```

### 3.3 日志统计概览（顶部统计卡片，可选）

在列表页顶部展示当日日志统计数据（快速概览）：

| 指标 | 说明 |
|------|------|
| 今日操作总数 | 当天0点至当前时刻的操作日志总量 |
| 今日失败操作数 | 今天执行失败的操作次数 |
| 活跃操作人数 | 今天产生过操作记录的账号数量 |
| 安全告警数 | 今天触发安全级日志的次数 |

### 3.4 日志导出功能（预留接口）

支持将当前筛选条件下的日志数据导出为 Excel 文件：

- 导出字段：所有列表显示字段
- 导出格式：`.xlsx`
- 导出上限：单次最多导出 50,000 条
- 文件命名：`操作日志_yyyyMMddHHmmss.xlsx`
- **当前版本预留空接口，不做前端实现**

---

## 四、日志级别枚举规范

### 4.1 日志级别定义

| 级别 | 枚举值 | 标签颜色 | 适用场景 |
|------|--------|----------|----------|
| 系统级 | SYSTEM | 蓝色（`#409eff`） | 系统自动触发的操作，如定时任务、自动流转、系统初始化 |
| 业务级 | BUSINESS | 绿色（`#67c23a`） | 用户主动发起的常规业务操作，如创建工单、更新状态、添加评论 |
| 安全级 | SECURITY | 橙色（`#e6a23c`） | 涉及权限变更、账号操作、敏感配置修改等安全敏感操作 |
| 错误级 | ERROR | 红色（`#f56c6c`） | 操作执行失败，系统产生异常的记录 |

### 4.2 日志级别分配规则

**系统级（SYSTEM）：**
- 企业微信 OAuth 回调处理
- Webhook 自动派发
- SLA 自动超期检测
- 定时通知发送

**业务级（BUSINESS）：**
- 工单创建、处理、关闭
- 缺陷信息更新
- 工作流配置变更
- 表单模板创建/编辑/删除
- 用户信息修改

**安全级（SECURITY）：**
- 用户登录 / 登出
- 密码修改
- 权限角色变更
- 账号启用 / 停用
- API 密钥生成 / 撤销

**错误级（ERROR）：**
- 任何接口返回异常（HTTP 5xx）
- 业务规则校验失败（如 SLA 策略冲突）
- 外部服务调用失败（企微 API 超时等）

---

## 五、所属应用枚举规范

| 应用名称 | 枚举值 | 说明 |
|----------|--------|------|
| 米多球基础 | MIDUO_BASE | 基础平台核心功能 |
| 米多后台 | MIDUO_BACKEND | 后台管理操作 |
| 米多球CRM | MIDUO_CRM | CRM 客户管理模块 |
| 工单系统 | TICKET_SYSTEM | 工单平台核心业务 |

---

## 六、接口设计规范（API 编号段：API000600-API000699）

### 6.1 接口列表

| 接口编号 | 接口名称 | HTTP方法 | 接口路径 | 说明 | 开发状态 |
|---------|---------|---------|---------|------|---------|
| API000600 | 分页查询操作日志 | GET | `/api/operation-log/page` | 多条件联合查询，带分页 | ⏳待开发 |
| API000601 | 获取操作日志详情 | GET | `/api/operation-log/detail/{id}` | 查询单条日志完整详情 | ⏳待开发 |
| API000602 | 获取日志统计概览 | GET | `/api/operation-log/statistics` | 今日各类日志统计数据 | ⏳待开发 |
| API000603 | 导出操作日志 | GET | `/api/operation-log/export` | 按条件导出日志为Excel（预留空接口） | ⏳预留 |
| API000604 | 获取操作模块枚举 | GET | `/api/operation-log/module/list` | 获取系统中所有操作模块的枚举列表 | ⏳待开发 |
| API000605 | 获取所属应用枚举 | GET | `/api/operation-log/app/list` | 获取所有注册应用的枚举列表 | ⏳待开发 |

### 6.2 分页查询接口详情（API000600）

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | Integer | 是 | 页码，从1开始 |
| pageSize | Integer | 是 | 每页条数，默认20 |
| startTime | String | 否 | 操作开始时间，格式 `yyyy-MM-dd HH:mm:ss` |
| endTime | String | 否 | 操作结束时间，格式 `yyyy-MM-dd HH:mm:ss` |
| accountId | Long | 否 | 操作账号ID，精确匹配 |
| operatorName | String | 否 | 操作人姓名，模糊匹配 |
| operatorIp | String | 否 | 操作人IP，前缀匹配 |
| logLevel | String | 否 | 日志级别枚举：SYSTEM/BUSINESS/SECURITY/ERROR |
| appCode | String | 否 | 所属应用枚举值 |
| moduleName | String | 否 | 操作模块名称，模糊匹配 |
| operationItem | String | 否 | 操作项名称，模糊匹配 |
| operationDetail | String | 否 | 操作详情关键词，模糊匹配 |
| executeResult | String | 否 | 执行结果枚举：SUCCESS/FAILURE |
| sortField | String | 否 | 排序字段，目前支持：operateTime（默认） |
| sortOrder | String | 否 | 排序方向：asc/desc（默认desc） |

**响应结构：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1234,
    "pageNum": 1,
    "pageSize": 20,
    "list": [
      {
        "id": 1,
        "operateTime": "2026-03-16 11:19:53",
        "accountId": 10,
        "operatorName": "张坤",
        "operatorIp": "81.68.72.59",
        "logLevel": "SYSTEM",
        "logLevelDesc": "系统级",
        "appCode": "MIDUO_BASE",
        "appName": "米多球基础",
        "moduleName": "登录模块",
        "requestPath": "/api/auth/wecom/callback",
        "operationItem": "企业微信登录接收回调",
        "executeResult": "SUCCESS",
        "executeResultDesc": "成功"
      }
    ]
  }
}
```

### 6.3 日志详情接口详情（API000601）

**路径参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 日志记录ID |

**响应结构：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "operateTime": "2026-03-16 11:19:53.456",
    "accountId": 10,
    "operatorName": "张坤",
    "operatorIp": "81.68.72.59",
    "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)...",
    "logLevel": "SYSTEM",
    "logLevelDesc": "系统级",
    "appCode": "MIDUO_BASE",
    "appName": "米多球基础",
    "moduleName": "登录模块",
    "requestPath": "/api/auth/wecom/callback",
    "requestMethod": "GET",
    "operationItem": "企业微信登录接收回调",
    "requestParams": "{\"code\":\"xxx\",\"state\":\"xxx\"}",
    "executeResult": "SUCCESS",
    "executeResultDesc": "成功",
    "costMillis": 312,
    "changeRecords": [
      {
        "fieldName": "最后登录时间",
        "beforeValue": "2026-03-15 18:00:00",
        "afterValue": "2026-03-16 11:19:53"
      }
    ],
    "errorCode": null,
    "errorMessage": null,
    "errorStack": null
  }
}
```

### 6.4 统计概览接口（API000602）

**响应结构：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todayTotalCount": 1247,
    "todayFailureCount": 23,
    "todayActiveUserCount": 36,
    "todaySecurityAlertCount": 5
  }
}
```

### 6.5 导出接口（API000603 预留）

**说明：** 当前版本仅预留接口定义，不实现具体导出逻辑。接口接收与分页查询相同的筛选参数（去掉 pageNum / pageSize），返回文件流。

---

## 七、数据库表设计

### 7.1 操作日志表：`sys_operation_log`

```sql
CREATE TABLE `sys_operation_log` (
  `id`               bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_id`       bigint(20) NOT NULL DEFAULT 0 COMMENT '操作账号ID（0表示系统自动操作）',
  `operator_name`    varchar(50) NOT NULL DEFAULT '' COMMENT '操作人姓名',
  `operator_ip`      varchar(50) NOT NULL DEFAULT '' COMMENT '操作人IP地址',
  `user_agent`       varchar(512) NOT NULL DEFAULT '' COMMENT '客户端User-Agent',
  `log_level`        varchar(20) NOT NULL DEFAULT 'BUSINESS' COMMENT '日志级别：SYSTEM/BUSINESS/SECURITY/ERROR',
  `app_code`         varchar(50) NOT NULL DEFAULT '' COMMENT '所属应用编码',
  `app_name`         varchar(100) NOT NULL DEFAULT '' COMMENT '所属应用名称',
  `module_name`      varchar(100) NOT NULL DEFAULT '' COMMENT '操作模块名称',
  `request_path`     varchar(255) NOT NULL DEFAULT '' COMMENT '接口请求路径',
  `request_method`   varchar(10) NOT NULL DEFAULT '' COMMENT 'HTTP请求方式',
  `operation_item`   varchar(200) NOT NULL DEFAULT '' COMMENT '操作项名称',
  `request_params`   text COMMENT '请求参数（JSON格式）',
  `change_records`   text COMMENT '变更记录（JSON格式，含字段名、变更前后值）',
  `execute_result`   varchar(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '执行结果：SUCCESS/FAILURE',
  `cost_millis`      int(11) NOT NULL DEFAULT 0 COMMENT '接口耗时（毫秒）',
  `error_code`       varchar(50) DEFAULT NULL COMMENT '错误码',
  `error_message`    varchar(512) DEFAULT NULL COMMENT '错误信息',
  `error_stack`      text COMMENT '异常堆栈摘要',
  `operate_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `create_time`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`        varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by`        varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted`          tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_account_id`    (`account_id`),
  KEY `idx_operate_time`  (`operate_time`),
  KEY `idx_log_level`     (`log_level`),
  KEY `idx_app_code`      (`app_code`),
  KEY `idx_execute_result`(`execute_result`),
  KEY `idx_deleted`       (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';
```

### 7.2 字段说明补充

| 字段 | 设计说明 |
|------|----------|
| `account_id` | 0 代表系统自动触发的操作（如定时任务） |
| `change_records` | 存储 JSON 数组，格式：`[{"fieldName":"","beforeValue":"","afterValue":""}]` |
| `request_params` | 敏感参数（密码、token等）在写入时自动脱敏，替换为 `***` |
| `cost_millis` | 通过 AOP 切面拦截，在接口执行完成后自动计算填写 |
| `error_stack` | 仅存储前500字符的异常栈摘要，避免字段过大 |
| `operate_time` | 记录实际业务操作发生时间（可能与create_time不同，如补录场景） |

---

## 八、AOP 自动记录日志方案

### 8.1 设计思路

通过 Spring AOP 切面 + 自定义注解的方式，自动拦截Controller层方法，无感知地写入操作日志，避免业务代码污染。

### 8.2 自定义注解 `@OperationLog`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 所属应用编码（与AppCodeEnum对应） */
    AppCodeEnum appCode() default AppCodeEnum.TICKET_SYSTEM;
    
    /** 操作模块名称 */
    String moduleName();
    
    /** 操作项名称 */
    String operationItem();
    
    /** 日志级别（默认业务级） */
    LogLevelEnum logLevel() default LogLevelEnum.BUSINESS;
    
    /** 是否记录请求参数（敏感接口可关闭） */
    boolean recordParams() default true;
    
    /** 是否记录变更内容 */
    boolean recordChanges() default false;
}
```

### 8.3 使用示例

```java
@OperationLog(
    moduleName = "工单管理",
    operationItem = "关闭工单",
    logLevel = LogLevelEnum.BUSINESS,
    recordChanges = true
)
@PutMapping("/close/{id}")
public CommonResult<Void> closeTicket(@PathVariable Long id, ...) {
    // 业务逻辑
}
```

---

## 九、前端页面设计规范

### 9.1 页面布局

```
┌─────────────────────────────────────────────────────────┐
│ 标题：星球日志                                            │
├─────────────────────────────────────────────────────────┤
│ [统计概览卡片区] 今日操作 | 失败操作 | 活跃用户 | 安全告警 │
├─────────────────────────────────────────────────────────┤
│ [搜索筛选区]                                              │
│ 操作时间: [开始时间] 至 [结束时间]  操作账号ID: [____]    │
│ 操作人: [____]  操作人IP: [____]  日志级别: [▼]           │
│ 所属应用: [▼]  操作模块: [____]   操作项: [____]          │
│ 操作详情: [____]  执行结果: [▼]       [搜索] [重置]       │
├─────────────────────────────────────────────────────────┤
│ [数据表格]                                                │
│ 操作时间↓ | 账号ID | 操作人 | IP | 级别 | 应用 | 模块 |  │
│           路径 | 操作项 | 结果 | 操作                    │
├─────────────────────────────────────────────────────────┤
│ 共 X 条  [10▼] [◀] [1][2]...[N] [▶]  跳至[_]页          │
└─────────────────────────────────────────────────────────┘
```

### 9.2 颜色规范

| 元素 | 样式规范 |
|------|----------|
| 系统级标签 | `background: #ecf5ff; color: #409eff; border-color: #b3d8ff` |
| 业务级标签 | `background: #f0f9eb; color: #67c23a; border-color: #c2e7b0` |
| 安全级标签 | `background: #fdf6ec; color: #e6a23c; border-color: #f5dab1` |
| 错误级标签 | `background: #fef0f0; color: #f56c6c; border-color: #fbc4c4` |
| 成功标签 | `background: #f0f9eb; color: #67c23a; border-color: #c2e7b0` |
| 失败标签 | `background: #fef0f0; color: #f56c6c; border-color: #fbc4c4` |

### 9.3 详情抽屉规范

- 宽度：640px，从右侧滑入
- 基础信息：使用 `el-descriptions` 组件，两列布局
- 请求参数：使用代码高亮组件（`highlight.js` 或内置pre标签）
- 变更记录：使用对比表格，变更前用浅红背景，变更后用浅绿背景
- 错误堆栈：使用可展开折叠的 `el-collapse`

---

## 十、性能与安全要求

### 10.1 性能要求

| 指标 | 要求 |
|------|------|
| 列表查询响应时间 | P95 < 500ms（索引优化后） |
| 日志写入方式 | 异步写入（使用线程池 + 队列），不阻塞主业务链路 |
| 日志写入延迟 | 允许最多5秒的写入延迟 |
| 日志存储周期 | 默认保留180天，超期数据归档或清理 |
| 单表数据量 | 预计日均2000条，180天约36万条，无需分表 |

### 10.2 安全要求

| 场景 | 处理规则 |
|------|----------|
| 密码字段 | 记录日志时自动替换为 `***`，禁止明文存储 |
| Token字段 | 仅保留前8位 + `***` |
| 手机号 | 中间4位脱敏，如 `138****8888` |
| 身份证号 | 中间部分脱敏 |
| 访问控制 | 日志模块仅系统管理员和超级管理员可访问 |

---

## 十一、验收标准

### 11.1 功能验收

- [x] 列表页按10种筛选条件组合查询正确
- [x] 分页功能正常，支持4种每页条数切换
- [x] 操作时间列支持升序/降序排序
- [x] 日志级别标签颜色正确显示
- [x] 执行结果标签颜色正确显示
- [x] 点击「详情」可正常打开抽屉，展示完整日志信息
- [x] 详情中变更记录对比表正常展示
- [x] 执行失败的日志详情中错误信息区正常显示
- [x] 统计概览卡片数据与实际数据一致
- [x] 导出接口预留，前端按钮暂时置灰不可用

### 11.2 性能验收

- [x] 36万条数据量下，带索引的列表查询响应 < 500ms
- [x] 异步写入日志不影响主业务接口响应时间（误差 < 10ms）

### 11.3 安全验收

- [x] 密码、token字段在日志中脱敏显示
- [x] 非管理员账号访问日志接口返回403

---

## 十二、版本规划

| 版本 | 功能范围 | 优先级 |
|------|----------|--------|
| v1.0（当前） | 日志列表查询、详情查看、统计概览、AOP自动采集、导出接口预留 | P0 |
| v1.1（后续） | 日志导出 Excel 实现、日志数据保留策略配置 | P1 |
| v1.2（后续） | 安全告警实时推送（企微通知）、异常操作自动标记 | P2 |
| v2.0（规划） | 日志分析看板（操作热力图、异常趋势图）、AI 异常识别 | P3 |

---

## 附录一：枚举值对照表

### 日志级别枚举（LogLevelEnum）

| 枚举值 | 中文描述 | 标签颜色 |
|--------|----------|----------|
| SYSTEM | 系统级 | 蓝色 |
| BUSINESS | 业务级 | 绿色 |
| SECURITY | 安全级 | 橙色 |
| ERROR | 错误级 | 红色 |

### 执行结果枚举（ExecuteResultEnum）

| 枚举值 | 中文描述 | 标签颜色 |
|--------|----------|----------|
| SUCCESS | 成功 | 绿色 |
| FAILURE | 失败 | 红色 |

### 应用编码枚举（AppCodeEnum）

| 枚举值 | 中文描述 |
|--------|----------|
| MIDUO_BASE | 米多球基础 |
| MIDUO_BACKEND | 米多后台 |
| MIDUO_CRM | 米多球CRM |
| TICKET_SYSTEM | 工单系统 |

---

## 附录二：关联文档

| 文档 | 路径 |
|------|------|
| 工单系统产品设计方案 | `miduo-md/business/工单系统产品设计方案.md` |
| 功能接口对应关系 | `miduo-md/workflow/功能接口对应关系.md` |
| 后端编码规范 | `.cursor/rules/backend-java-standards.mdc` |
| 实体类字段规范 | `.cursor/rules/entity-base-fields.mdc` |
| 数据库SQL目录 | `miduo-md/database/` |
