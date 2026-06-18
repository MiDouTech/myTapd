# 公开工单详情 SLA 倒计时说明

> **业务模块**：工单系统公开页  
> **关联页面**：`/open/ticket/{ticketNo}`  
> **关联接口**：`GET /api/open/ticket/{ticketNo}`（既有公开详情接口，扩展返回字段，不新增接口）

---

## 一、背景

企微或外部链接打开的公开工单详情页，目前只能看到工单基础信息、客户信息和评论。  
对处理人来说，缺少一个像“外卖还剩多久送达”的 SLA 倒计时，不容易判断当前工单还剩多少响应/解决时间。

## 二、目标

1. 公开详情页展示小型 SLA 时长计时器。
2. 同时展示响应 SLA 和解决 SLA。
3. 运行中的 SLA 只在工作时间内每秒倒计时刷新，非工作时间停住。
4. 暂停、完成、超时等状态用文字说明，不误导用户。

## 三、非目标

1. 不新增接口，只扩展现有公开详情接口返回值。
2. 不改变 SLA 后台超时判定规则，仍以 `sla_timer.threshold_minutes`、`elapsed_minutes` 和工作时间计算器为准。
3. 不在公开页提供 SLA 管理或工单处理操作。

## 四、字段设计

公开详情接口新增：

| 字段 | 类型 | 说明 |
|---|---|---|
| `slaTimers` | `Array` | 当前工单的 SLA 计时器列表 |
| `slaTimers[].timerType` | `String` | `RESPONSE` 响应 / `RESOLVE` 解决 |
| `slaTimers[].timerTypeLabel` | `String` | 中文类型名 |
| `slaTimers[].status` | `String` | `RUNNING/PAUSED/COMPLETED/BREACHED` |
| `slaTimers[].statusLabel` | `String` | 中文状态名 |
| `slaTimers[].thresholdMinutes` | `Integer` | SLA 总时限分钟数 |
| `slaTimers[].elapsedMinutes` | `Integer` | 已消耗分钟数 |
| `slaTimers[].elapsedSeconds` | `Long` | 已消耗工作秒数，完成态用于展示总用时 |
| `slaTimers[].remainingSeconds` | `Long` | 后端计算的剩余秒数 |
| `slaTimers[].deadline` | `Date` | 截止时间；完成/超时态为空，避免公开页误展示历史截止 |
| `slaTimers[].showDeadline` | `Boolean` | 是否允许展示截止时间 |
| `slaTimers[].breached` | `Boolean` | 是否已超时 |
| `workingTime.workTimeStart` | `String` | 工作开始时间，如 `09:00` |
| `workingTime.workTimeEnd` | `String` | 工作结束时间，如 `18:00` |
| `workingTime.workingDays` | `Array<Integer>` | 工作日，1=周一，7=周日 |
| `serverTime` | `Date` | 后端返回数据时的服务器时间 |

## 五、展示规则

| 状态 | 公开页展示 |
|---|---|
| RUNNING + 工作时间内 | 显示“剩余 HH:mm:ss”并每秒刷新 |
| RUNNING + 非工作时间 | 显示“非工作时间，剩余 HH:mm:ss”，倒计时停住 |
| PAUSED | 显示“暂停中 HH:mm:ss”，不跳秒 |
| COMPLETED | 显示“已完成，用时 HH:mm:ss”，接口 `deadline=null` 且 `showDeadline=false`，不展示截止时间，避免误解 |
| BREACHED | 显示“已超时” |

## 六、验收标准

- [ ] 打开公开工单详情页时，标题卡片下方出现小型“SLA”条，不再占用大块空间。
- [ ] 有 SLA 的工单展示“首次响应”和“解决”两条计时信息。
- [ ] 工作时间内运行中的计时器每秒刷新倒计时。
- [ ] 非工作时间打开页面时，运行中计时器不继续减少。
- [ ] 超时计时器显示“已超时”，完成计时器显示“已完成，用时 xx”。
- [ ] 已完成计时器不再展示“截止 yyyy-MM-dd HH:mm”作为主要信息。
- [ ] 无 SLA 的工单不展示空卡片。

---

## 七、SLA 截止时间时区修复（2026-06-18补充）

### 7.1 问题

当服务器 JVM 默认时区不是业务时区时，后台用 `LocalDateTime.now()` 计算 SLA 截止时间会发生偏移。  
例如业务时间 `2026-06-18 14:40` 创建、首次响应 `1 分钟`，如果 JVM 按 UTC 计算，会被当成 `06:40`，早于 `09:00` 工作开始，于是顺延到 `09:01 UTC`，最终页面/数据库展示成 `17:01`。

### 7.2 修复方案

- SLA 的“当前时间”统一读取 `system_config(BASIC/timezone)`，默认 `Asia/Shanghai`。
- `Date -> LocalDateTime` 与 `LocalDateTime -> Date` 转换统一使用业务时区。
- `WorkingTimeCalculator.calculateDeadline(...)` 继续按 `WORKING_TIME` 配置计算工作时间，但输入时间已统一为业务本地时间。

### 7.3 验收样例

| 创建时间 | 工作时间 | SLA时限 | 预期截止 |
|---|---|---:|---|
| 2026-06-18 14:40:33 | 09:00-18:00 | 1 分钟 | 2026-06-18 14:41:33 |
| 2026-06-18 18:10:00 | 09:00-18:00 | 1 分钟 | 下一个工作日 09:01:00 |

### 7.4 历史数据

该修复只保证新建/后续重算的 SLA 截止时间正确。  
历史已经落错的 `sla_timer.deadline` 需要用一次性 SQL 按业务规则修复，或通过重新触发 SLA 重算逻辑修复。
