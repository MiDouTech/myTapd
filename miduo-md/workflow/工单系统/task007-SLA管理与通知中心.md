# Task007：SLA 管理与通知中心

> **业务模块**：工单系统  
> **依赖**：Task004、Task005  
> **预估工时**：5天  
> **对应产品文档**：4.7 SLA 管理、4.8 通知中心

---

## 一、任务目标

实现 SLA 策略配置、工作时间计算、定时检查与预警/超时升级；建立站内通知、企微应用消息、企微群推送、邮件等多渠道通知中心，支持用户通知偏好与消息合并。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 | 状态 |
|------|--------|-----------|------|
| 1 | SLA 策略配置 | `ticket-application/.../sla/SlaApplicationService.java` + `SlaPolicyController.java` | ✅完成 |
| 2 | 工作时间计算 | `ticket-application/.../sla/WorkingTimeCalculator.java`（排除非工作时间、节假日） | ✅完成 |
| 3 | SLA 计时器 | `ticket-application/.../sla/SlaTimerService.java` + `SlaTimerPO.java` | ✅完成 |
| 4 | 定时检查任务 | `ticket-job/.../handler/SlaCheckJobHandler.java` 每分钟扫描 | ✅完成 |
| 5 | 预警与升级 | `SlaLevel` 枚举 + `SlaWarningEvent/SlaBreachedEvent` 事件 | ✅完成 |
| 6 | SLA 暂停规则 | `SlaTimerService.pauseTimers()` / `resumeTimers()` | ✅完成 |
| 7 | 站内通知 | `NotificationController.java` + `NotificationWebSocketHandler.java` | ✅完成 |
| 8 | 通知编排器 | `ticket-application/.../notification/NotificationOrchestrator.java` | ✅完成 |
| 9 | 多渠道发送器 | `SiteNotificationSender` / `WecomAppMessageSender` / `WecomGroupWebhookSender` / `EmailSender` | ✅完成 |
| 10 | 消息合并 | `NotificationOrchestrator.shouldAggregate()` 通过 Redis 实现5分钟合并 | ✅完成 |
| 11 | 通知偏好 | `NotificationPreferencePO` + `NotificationApplicationService.getPreferences/updatePreferences` | ✅完成 |
| 12 | 催办 | `TicketUrgeController.java` + `TicketUrgedEvent` | ✅完成 |

## 三、接口清单

| 接口编号 | 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|------|
| API000001 | SLA 策略列表 | GET | /api/sla/policy/list | SLA 策略列表 |
| API000002 | SLA 策略创建 | POST | /api/sla/policy/create | 新增策略 |
| API000003 | SLA 策略更新 | PUT | /api/sla/policy/update | 更新策略 |
| API000004 | 通知列表 | GET | /api/notification/page | 站内通知分页 |
| API000005 | 通知已读 | PUT | /api/notification/read/{id} | 标记已读 |
| API000006 | 全部标记已读 | PUT | /api/notification/read/all | 全部标记已读 |
| API000007 | 未读数量 | GET | /api/notification/unread/count | 未读通知数量 |
| API000008 | 通知偏好 | GET | /api/notification/preference | 获取偏好 |
| API000009 | 通知偏好更新 | PUT | /api/notification/preference/update | 更新偏好 |
| API000029 | 催办 | POST | /api/ticket/urge/{id} | 催办工单（默认处理人 + 可选追加） |
| - | WebSocket | - | /ws/notification | 实时通知推送 |

## 四、验收标准

- [ ] SLA 按工作时间正确计时，节假日不计入  
- [ ] 预警与超时事件正确触发并通知  
- [ ] 站内通知与企微推送可收到  
- [ ] 消息合并与用户偏好生效  

## 五、产出说明

SLA 与通知能力就绪后，工单处理时效可度量，用户可多渠道接收提醒。

---

## 六、缺陷等级驱动解决时限调整（2026-06-14补充）

### 6.1 背景

缺陷工单由客成创建时通常还没有测试结论，因此客成无法在新建工单阶段准确填写 `P0/P1/P2/P3/P4` 缺陷等级。  
响应时限继续使用“工单分类绑定的 SLA 策略”，保证客成建单后能立即开始响应计时；解决时限等测试填写缺陷等级后再动态调整。

### 6.2 规则

| 缺陷等级 | 解决时限 | 说明 |
|---|---:|---|
| P0 | 60 分钟 | 1 小时内解决或实施有效临时方案 |
| P1 | 360 分钟 | 6 小时内解决或实施有效临时方案 |
| P2 | 480 分钟 | 1 个工作日内解决或转为临时处理状态 |
| P3 | 1440 分钟 | 3 个工作日内解决或转为临时处理状态 |
| P4 | 1440 分钟 | 3 个工作日内解决或转为临时处理状态 |

> 当前 SLA 计时统一沿用系统工作时间计算器；`480 分钟 = 1 个工作日`、`1440 分钟 = 3 个工作日` 与既有 SLA 种子策略保持一致。

### 6.3 触发点

1. 测试在「确认缺陷转开发」流转弹窗中填写或带出缺陷等级时，同步调整该工单的 `RESOLVE` 计时器。
2. 测试在「测试信息」页手动保存/修改缺陷等级时，同步调整该工单的 `RESOLVE` 计时器。
3. `RESPONSE` 计时器不因缺陷等级变化而调整，因为新建时客成不知道缺陷等级。

### 6.4 兼容与回滚

- 不新增接口、不新增数据库字段。
- 已完成的解决计时器不再调整。
- 调整时保留已消耗工作时间，只重算剩余时限；如果已消耗时间超过新等级时限，则该解决计时器进入超时状态。
- 回滚时移除缺陷等级保存后的解决计时器重算逻辑即可，历史 SLA 数据仍保留在 `sla_timer` 表。
