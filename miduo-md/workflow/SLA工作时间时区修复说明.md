# SLA 工作时间时区修复说明

> **类型**：缺陷修复（不改变对外 API 契约）  
> **关联模块**：`WorkingTimeCalculator`、`SlaTimerService`

---

## 一、背景

用户反馈：工单 10:33 创建、首次响应 SLA 30 分钟，截止时间显示 17:30，与直觉（11:03）不符。

## 二、根因

1. 页面创建时间、Jackson 序列化使用 `Asia/Shanghai`（或系统基础配置时区）。
2. `WorkingTimeCalculator` / `SlaTimerService` 使用 `LocalDateTime.now()` 与 `ZoneId.systemDefault()`，容器 JVM 常为 UTC。
3. 10:33 北京时间被当作 02:33 UTC，早于「上班时间 09:00」，SLA 从 09:00 UTC（17:00 北京）起算 30 分钟 → 截止 17:30 北京。

## 三、目标

- SLA 工作时间计算与页面展示使用同一业务时区（`system_config` BASIC/timezone，默认 Asia/Shanghai）。
- 运行中计时器在定时检查任务中按剩余工作分钟重算 `deadline`，历史错误截止可自动纠正。

## 四、非目标

- 不改变 SLA 策略、工作时间配置项含义。
- 不新增对外接口。

## 五、验收标准

- [ ] 工作日上午 10:33 创建、30 分钟响应 SLA 的工单，截止时间约为 11:03。
- [ ] 容器 `TZ=UTC` 时，SLA 截止与创建时间仍按业务时区一致。
- [ ] 已存在错误 deadline 的运行中计时器，在 SLA 检查任务执行后 deadline 被修正。
