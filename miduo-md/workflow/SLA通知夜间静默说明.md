# SLA 通知夜间静默说明

## 背景

SLA 预警/超时可能在夜间触发，影响休息。需在固定时段内延迟推送。

## 规则

| 项目 | 说明 |
|------|------|
| 静默时段 | 每日 **22:00（含）** 至 **次日 09:00（不含）** |
| 延迟发送 | 静默期内触发的 SLA 预警/超时，写入 `sla_notification_pending`，在 **下一次 09:00** 由定时任务发出 |
| 适用范围 | 仅 **SLA_WARNING**、**SLA_BREACHED**（站内 + 企微应用 + 企微群） |
| 不受影响 | 催办、分派、状态变更等其他通知 |

## 配置项（system_config / SLA 分组）

| config_key | 默认值 | 说明 |
|------------|--------|------|
| `sla_notify_quiet_start` | 22:00 | 静默开始 |
| `sla_notify_quiet_end` | 09:00 | 静默结束（到点可发） |

时区读取 `timezone`（BASIC 分组，默认 `Asia/Shanghai`）。

## 实现要点

- `SlaNotificationQuietHours`：判断静默与计算计划发送时间
- `SlaNotificationDispatchService`：立即发送或入队
- `SlaNotificationFlushJobHandler`：每分钟扫描到期队列并发送

## 验收

1. 在 23:00 人为触发 SLA 超时 → 当晚无企微推送，库中有 `PENDING` 记录且 `scheduled_send_at` 为次日 09:00
2. 次日 09:00–09:01 → 收到延迟通知，状态变为 `SENT`
3. 在 14:00 触发 SLA 超时 → 立即收到通知
