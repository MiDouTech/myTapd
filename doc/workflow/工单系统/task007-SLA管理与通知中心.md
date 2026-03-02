# Task007：SLA 管理与通知中心

> **业务模块**：工单系统  
> **依赖**：Task004、Task005  
> **预估工时**：5天  
> **对应产品文档**：4.7 SLA 管理、4.8 通知中心

---

## 一、任务目标

实现 SLA 策略配置、工作时间计算、定时检查与预警/超时升级；建立站内通知、企微应用消息、企微群推送、邮件等多渠道通知中心，支持用户通知偏好与消息合并。

## 二、交付物清单

| 序号 | 交付物 | 路径/说明 |
|------|--------|-----------|
| 1 | SLA 策略配置 | 按优先级配置响应/解决时限 |
| 2 | 工作时间计算 | WorkingTimeCalculator（排除非工作时间、节假日） |
| 3 | SLA 计时器 | sla_timer 表、运行/暂停/超时状态 |
| 4 | 定时检查任务 | SlaCheckJob 每分钟扫描，触发预警/超时事件 |
| 5 | 预警与升级 | 50% 绿、25%~50% 黄、<25% 橙、超时红，通知层级递增 |
| 6 | SLA 暂停规则 | 已挂起、待验收状态暂停计时 |
| 7 | 站内通知 | 通知列表 API、WebSocket 实时推送 |
| 8 | 通知编排器 | NotificationOrchestrator 按事件类型分发 |
| 9 | 多渠道发送器 | SiteNotificationSender、WecomAppMessageSender、WecomGroupWebhookSender、EmailSender |
| 10 | 消息合并 | 同一工单 5 分钟内多次变更合并为一条 |
| 11 | 通知偏好 | notification_preference 用户配置 |
| 12 | 催办 | 创建人催办，通知处理人 |

## 三、接口清单（需填接口编号）

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| SLA 策略列表 | GET | /api/sla/policy/list | SLA 策略列表 |
| SLA 策略创建 | POST | /api/sla/policy/create | 新增策略 |
| 通知列表 | GET | /api/notification/page | 站内通知分页 |
| 通知已读 | PUT | /api/notification/read/{id} | 标记已读 |
| 通知偏好 | GET | /api/notification/preference | 获取偏好 |
| 通知偏好更新 | PUT | /api/notification/preference/update | 更新偏好 |
| 催办 | POST | /api/ticket/urge/{id} | 催办工单 |
| WebSocket | - | /ws/notification | 实时通知推送 |

## 四、验收标准

- [ ] SLA 按工作时间正确计时，节假日不计入  
- [ ] 预警与超时事件正确触发并通知  
- [ ] 站内通知与企微推送可收到  
- [ ] 消息合并与用户偏好生效  

## 五、产出说明

SLA 与通知能力就绪后，工单处理时效可度量，用户可多渠道接收提醒。
