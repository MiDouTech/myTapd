# 公开工单详情 SLA 倒计时说明

> **业务模块**：工单系统公开页  
> **关联页面**：`/open/ticket/{ticketNo}`  
> **关联接口**：`GET /api/open/ticket/{ticketNo}`（既有公开详情接口，扩展返回字段，不新增接口）

---

## 一、背景

企微或外部链接打开的公开工单详情页，目前只能看到工单基础信息、客户信息和评论。  
对处理人来说，缺少一个像“外卖还剩多久送达”的 SLA 倒计时，不容易判断当前工单还剩多少响应/解决时间。

## 二、目标

1. 公开详情页展示 SLA 时长计时器。
2. 同时展示响应 SLA 和解决 SLA。
3. 运行中的 SLA 每秒倒计时刷新。
4. 暂停、完成、超时等状态用文字说明，不误导用户。

## 三、非目标

1. 不新增接口，只扩展现有公开详情接口返回值。
2. 不改变 SLA 计算规则，仍以 `sla_timer.deadline`、`threshold_minutes`、`elapsed_minutes` 为准。
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
| `slaTimers[].remainingSeconds` | `Long` | 后端计算的剩余秒数 |
| `slaTimers[].deadline` | `Date` | 截止时间 |
| `slaTimers[].breached` | `Boolean` | 是否已超时 |

## 五、展示规则

| 状态 | 公开页展示 |
|---|---|
| RUNNING | 显示“剩余 HH:mm:ss”并每秒刷新 |
| PAUSED | 显示“暂停中”，展示剩余时长但不跳秒 |
| COMPLETED | 显示“已完成” |
| BREACHED | 显示“已超时” |

## 六、验收标准

- [ ] 打开公开工单详情页时，基础信息上方/附近出现“SLA倒计时”卡片。
- [ ] 有 SLA 的工单展示“首次响应”和“解决”两条计时信息。
- [ ] 运行中的计时器每秒刷新倒计时。
- [ ] 超时计时器显示“已超时”，完成计时器显示“已完成”。
- [ ] 无 SLA 的工单不展示空卡片。
