# SLA 功能自测检查方案与测试用例

> **版本**：v1.0  
> **日期**：2026-03-20  
> **覆盖模块**：SLA策略管理、SLA计时器生命周期、工作时间计算、预警与超时、看板统计、通知中心、前端UI  
> **对应任务**：task007-SLA管理与通知中心  
> **测试负责人**：待分配  

---

## 一、自测检查清单（Self-Check Checklist）

在执行具体测试用例之前，先逐项过一遍以下环境与功能基线，确认全部通过再进入用例执行。

### 1.1 环境准备

| 编号 | 检查项 | 预期结果 | 状态 |
|------|--------|----------|------|
| ENV-01 | MySQL 容器正常运行，端口 3306 可访问 | `docker ps` 中 ticket-mysql 状态为 Up | ⬜ |
| ENV-02 | Redis 容器正常运行，端口 6379 可访问 | `docker ps` 中 ticket-redis 状态为 Up | ⬜ |
| ENV-03 | 后端 Spring Boot 服务正常启动（端口 8080） | 启动日志无 ERROR，`/api/health` 返回 200 | ⬜ |
| ENV-04 | 前端 Vite 服务正常启动（端口 5173） | 页面可访问，控制台无红色报错 | ⬜ |
| ENV-05 | Flyway 数据库迁移全部执行成功（V1–V9） | 启动日志中无 migration 错误 | ⬜ |
| ENV-06 | 初始化 4 条 SLA 种子数据存在 | `SELECT * FROM sla_policy` 返回 4 行 | ⬜ |
| ENV-07 | 已有可用的系统工作时间配置 | `system_config` 表中存在 `working_time_start`、`working_time_end`、`working_days` | ⬜ |
| ENV-08 | 当前用户已完成企微登录，JWT Token 有效 | 调用任意接口返回 200，非 401 | ⬜ |

### 1.2 SLA 策略管理基线

| 编号 | 检查项 | 预期结果 | 状态 |
|------|--------|----------|------|
| BASE-SLA-01 | 前端菜单中存在"SLA管理"入口 | 路由 `/manage/sla` 可正常访问 | ⬜ |
| BASE-SLA-02 | 页面加载后自动请求 `GET /api/sla/policy/list` | Network 面板中请求状态 200 | ⬜ |
| BASE-SLA-03 | 4 条种子策略在表格中全部显示 | 紧急/高/中/低 四行均展示 | ⬜ |
| BASE-SLA-04 | 每行策略显示优先级标签颜色有差异 | 紧急=红色，高=橙色，中=黄色，低=蓝色（或类似区分） | ⬜ |

### 1.3 SLA 计时器基线

| 编号 | 检查项 | 预期结果 | 状态 |
|------|--------|----------|------|
| BASE-TIMER-01 | 创建属于已绑定 SLA 策略的工单分类的工单后，`sla_timer` 表中有新记录 | 2 条 timer（RESPONSE + RESOLVE），状态均为 RUNNING | ⬜ |
| BASE-TIMER-02 | 定时任务 `SlaCheckJobHandler` 每分钟执行 | 应用日志中每分钟有 SLA check 日志输出 | ⬜ |

### 1.4 通知基线

| 编号 | 检查项 | 预期结果 | 状态 |
|------|--------|----------|------|
| BASE-NOTIF-01 | 通知中心页面可访问 | 路由 `/notification` 可正常加载 | ⬜ |
| BASE-NOTIF-02 | 未读数徽标在导航栏显示 | `GET /api/notification/unread/count` 返回值 ≥ 0 | ⬜ |
| BASE-NOTIF-03 | WebSocket 连接建立成功 | 浏览器控制台 Network - WS 中有 `/ws/notification` 连接且状态为 101 | ⬜ |

---

## 二、测试用例（Test Cases）

### 2.1 SLA 策略管理

#### TC-SLA-001：查询 SLA 策略列表

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-SLA-001 |
| **用例名称** | 查询 SLA 策略列表 |
| **对应接口** | GET /api/sla/policy/list（API000001） |
| **前置条件** | 系统中存在 4 条初始 SLA 策略 |
| **测试步骤** | 1. 携带有效 JWT 发送 `GET /api/sla/policy/list` |
| **预期结果** | HTTP 200；`data` 为数组，长度 ≥ 4；每条记录包含 `id`、`name`、`priority`、`priorityLabel`、`responseTime`、`resolveTime`、`warningPct`、`criticalPct`、`isActive`、`createTime` 字段；按 id 升序排列 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

```bash
curl -X GET "http://localhost:8080/api/sla/policy/list" \
  -H "Authorization: Bearer ${TOKEN}"
```

---

#### TC-SLA-002：创建 SLA 策略（正常路径）

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-SLA-002 |
| **用例名称** | 创建 SLA 策略（所有字段合法） |
| **对应接口** | POST /api/sla/policy/create（API000002） |
| **前置条件** | 当前不存在名为"自定义测试策略"的 SLA 策略 |
| **测试步骤** | 1. 携带 JWT 发送以下请求体 |
| **预期结果** | HTTP 200；`data` 包含新生成的 `id`；数据库 `sla_policy` 表新增一行，`deleted=0`、`is_active=1` |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

```bash
curl -X POST "http://localhost:8080/api/sla/policy/create" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "自定义测试策略",
    "priority": "HIGH",
    "responseTime": 60,
    "resolveTime": 480,
    "warningPct": 75,
    "criticalPct": 90,
    "description": "用于自测的策略"
  }'
```

**验证 SQL**：
```sql
SELECT * FROM sla_policy WHERE name = '自定义测试策略';
-- 预期：1 行，is_active=1，deleted=0
```

---

#### TC-SLA-003：创建 SLA 策略（缺少必填字段）

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-SLA-003 |
| **用例名称** | 创建 SLA 策略时未填写必填字段 `name` |
| **对应接口** | POST /api/sla/policy/create（API000002） |
| **前置条件** | 无 |
| **测试步骤** | 1. 发送不含 `name` 字段的请求体 |
| **预期结果** | HTTP 400 或业务错误码；响应体包含参数校验错误信息；数据库中无新增记录 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

```bash
curl -X POST "http://localhost:8080/api/sla/policy/create" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "priority": "HIGH",
    "responseTime": 60,
    "resolveTime": 480
  }'
```

---

#### TC-SLA-004：创建 SLA 策略（responseTime 为负数）

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-SLA-004 |
| **用例名称** | 创建 SLA 策略时响应时限为非法值 |
| **对应接口** | POST /api/sla/policy/create（API000002） |
| **前置条件** | 无 |
| **测试步骤** | 1. 发送 `responseTime: -10` |
| **预期结果** | HTTP 400 或业务错误；响应体有明确的参数错误提示 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

```bash
curl -X POST "http://localhost:8080/api/sla/policy/create" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"非法测试","priority":"LOW","responseTime":-10,"resolveTime":240}'
```

---

#### TC-SLA-005：更新 SLA 策略（正常路径）

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-SLA-005 |
| **用例名称** | 更新已存在 SLA 策略的响应时限 |
| **对应接口** | PUT /api/sla/policy/update（API000003） |
| **前置条件** | 策略 ID=1（紧急SLA策略）存在 |
| **测试步骤** | 1. 携带 JWT 发送更新请求，修改 `responseTime` 为 20 |
| **预期结果** | HTTP 200；数据库中 id=1 的策略 `response_time=20`，`update_time` 已更新 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

```bash
curl -X PUT "http://localhost:8080/api/sla/policy/update" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "responseTime": 20
  }'
```

**验证 SQL**：
```sql
SELECT id, name, response_time, update_time FROM sla_policy WHERE id = 1;
-- 预期：response_time = 20
```

---

#### TC-SLA-006：禁用 SLA 策略

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-SLA-006 |
| **用例名称** | 将已启用的 SLA 策略切换为禁用状态 |
| **对应接口** | PUT /api/sla/policy/update（API000003） |
| **前置条件** | 存在 `is_active=1` 的策略 |
| **测试步骤** | 1. 发送 `{ "id": 1, "isActive": 0 }` |
| **预期结果** | HTTP 200；数据库中该策略 `is_active=0`；前端列表中该策略的启用开关变为关闭状态 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

```bash
curl -X PUT "http://localhost:8080/api/sla/policy/update" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "isActive": 0}'
```

---

#### TC-SLA-007：启用 SLA 策略

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-SLA-007 |
| **用例名称** | 将已禁用的 SLA 策略重新启用 |
| **对应接口** | PUT /api/sla/policy/update（API000003） |
| **前置条件** | 存在 `is_active=0` 的策略（执行 TC-SLA-006 后） |
| **测试步骤** | 1. 发送 `{ "id": 1, "isActive": 1 }` |
| **预期结果** | HTTP 200；数据库中该策略 `is_active=1` |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-SLA-008：更新不存在的 SLA 策略

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-SLA-008 |
| **用例名称** | 更新一个不存在的策略 ID |
| **对应接口** | PUT /api/sla/policy/update（API000003） |
| **前置条件** | ID=99999 的策略不存在 |
| **测试步骤** | 1. 发送 `{ "id": 99999, "responseTime": 30 }` |
| **预期结果** | 接口返回业务错误，提示策略不存在；HTTP 非 200 或 data 为 null |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

### 2.2 SLA 计时器生命周期

#### TC-TIMER-001：创建工单时自动启动 SLA 计时器

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-TIMER-001 |
| **用例名称** | 工单创建后 SLA 计时器自动启动 |
| **前置条件** | 工单分类已绑定 SLA 策略（如 sla_policy_id=1），系统工作时间已配置 |
| **测试步骤** | 1. 创建一张属于该分类的工单<br>2. 查询 `sla_timer` 表 |
| **预期结果** | 新增 2 条 timer 记录：`timer_type=RESPONSE` 和 `timer_type=RESOLVE`；`status=RUNNING`；`start_at` 为当前时间；`deadline` 为按工作时间计算的截止时间 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

**验证 SQL**：
```sql
SELECT timer_type, status, threshold_minutes, start_at, deadline
FROM sla_timer
WHERE ticket_id = ${NEW_TICKET_ID}
ORDER BY timer_type;
-- 预期：2 行，status=RUNNING
```

---

#### TC-TIMER-002：工单进入等待状态时计时器暂停

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-TIMER-002 |
| **用例名称** | 工单流转到"挂起/等待确认"状态后计时器暂停 |
| **前置条件** | 存在 `RUNNING` 状态的 SLA 计时器 |
| **测试步骤** | 1. 将工单状态流转至含 `slaAction=PAUSE` 的状态（如 suspended）<br>2. 查询 `sla_timer` 表 |
| **预期结果** | 对应工单的 timer 状态变为 `PAUSED`；`pause_at` 已记录；`elapsed_minutes` 有合理的累计值（≥0） |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-TIMER-003：工单从等待状态恢复后计时器续计

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-TIMER-003 |
| **用例名称** | 工单从暂停状态恢复后计时器继续倒计时 |
| **前置条件** | 存在 `PAUSED` 状态的 SLA 计时器（执行 TC-TIMER-002 后） |
| **测试步骤** | 1. 将工单流转回活跃处理状态（含 `slaAction=START_RESOLVE`）<br>2. 查询 `sla_timer` 表 |
| **预期结果** | timer 状态恢复为 `RUNNING`；`start_at` 更新为当前时间；`deadline` 基于剩余时间（`threshold - elapsed`）重新计算；`elapsed_minutes` 保留之前的累计值 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-TIMER-004：首次响应后 RESPONSE 计时器完成

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-TIMER-004 |
| **用例名称** | 处理人首次响应工单后，RESPONSE 类型计时器标记完成 |
| **前置条件** | 工单处于 RESPONSE timer RUNNING 状态 |
| **测试步骤** | 1. 处理人执行首次响应操作（如状态流转至 `processing`，含 `START_RESOLVE`）<br>2. 查询 `sla_timer` 表 |
| **预期结果** | `timer_type=RESPONSE` 的记录 `status=COMPLETED`，`completed_at` 有值；`timer_type=RESOLVE` 的记录仍为 `RUNNING` |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-TIMER-005：工单解决/关闭后所有计时器停止

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-TIMER-005 |
| **用例名称** | 工单进入终止状态后全部计时器完成 |
| **前置条件** | 工单处于活跃状态，存在 RUNNING 的 SLA 计时器 |
| **测试步骤** | 1. 将工单流转至终止状态（completed/closed/rejected，含 `slaAction=STOP`）<br>2. 查询 `sla_timer` 表 |
| **预期结果** | 所有该工单的 timer 记录 `status=COMPLETED`（或已是 BREACHED）；`completed_at` 均有值 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

### 2.3 工作时间计算

#### TC-WTIME-001：截止时间在工作时间内正确计算

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-WTIME-001 |
| **用例名称** | 工作时间内创建工单，deadline 计算正确 |
| **前置条件** | 工作时间配置：09:00–18:00，周一至周五；使用响应时限 60 分钟的 SLA 策略 |
| **测试步骤** | 1. 在工作日 09:30 创建工单<br>2. 查询 `sla_timer` 中 RESPONSE timer 的 deadline |
| **预期结果** | `deadline` = 当天 10:30（09:30 + 60 分钟） |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-WTIME-002：工作日结束后创建工单，deadline 跨天

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-WTIME-002 |
| **用例名称** | 下班时间创建工单时 deadline 顺延至次日 |
| **前置条件** | 工作时间配置：09:00–18:00，周一至周五；使用响应时限 60 分钟的 SLA |
| **测试步骤** | 1. 在工作日 17:30 创建工单<br>2. 查询 RESPONSE timer 的 `deadline` |
| **预期结果** | `deadline` = 次工作日 09:30（17:30 开始，30分钟到18:00，剩余 30 分钟顺延至次日 09:00+30min=09:30） |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-WTIME-003：周五下班后创建工单，deadline 跳过周末

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-WTIME-003 |
| **用例名称** | 周末不计入 SLA 工作时间 |
| **前置条件** | 工作时间配置周一至周五；使用响应时限 60 分钟的 SLA |
| **测试步骤** | 1. 在周五 17:30 创建工单<br>2. 查询 RESPONSE timer 的 `deadline` |
| **预期结果** | `deadline` = 下周一 09:30（周末不计时，剩余 30 分钟放到周一工作时间开始后计算） |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

### 2.4 SLA 预警与超时检测

#### TC-WARN-001：达到预警阈值时触发 SLA_WARNING 通知

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-WARN-001 |
| **用例名称** | 计时器使用率达到 warningPct（默认75%）时发出预警通知 |
| **前置条件** | 工单存在 RUNNING 状态计时器；策略 warningPct=75，threshold=60 分钟 |
| **测试步骤** | 1. 等待计时器运行至 45 分钟（75% 进度），或手动将 `start_at` 修改为 45 分钟前<br>2. 等待定时任务（每 60 秒）执行一次<br>3. 查询 `sla_timer` 和通知表 |
| **预期结果** | `sla_timer.is_warned=1`；`notification` 表中有 `type=SLA_WARNING` 的记录，接收人为工单处理人；WebSocket 推送了预警消息 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

**模拟预警的 SQL（测试辅助）**：
```sql
-- 将 RESPONSE timer 的 start_at 向前移动，使 elapsed_minutes 达到 75% 阈值
UPDATE sla_timer
SET start_at = DATE_SUB(NOW(), INTERVAL 45 MINUTE)
WHERE ticket_id = ${TICKET_ID} AND timer_type = 'RESPONSE';
```

---

#### TC-WARN-002：预警通知只发送一次（is_warned 幂等）

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-WARN-002 |
| **用例名称** | 已发送过预警的 timer 再次扫描时不重复发送通知 |
| **前置条件** | `sla_timer.is_warned=1` 的记录（执行 TC-WARN-001 后） |
| **测试步骤** | 1. 等待定时任务再次执行<br>2. 查询通知表中 SLA_WARNING 记录数量 |
| **预期结果** | 通知表中同一工单的 SLA_WARNING 通知数量不增加（仍为 1 条） |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-BREACH-001：超时后触发 SLA_BREACHED 事件

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-BREACH-001 |
| **用例名称** | 计时器超过阈值时间后标记为 BREACHED |
| **前置条件** | 工单存在 RUNNING 状态计时器；策略 threshold=60 分钟 |
| **测试步骤** | 1. 将 `start_at` 修改为 65 分钟前（超过 60 分钟阈值）<br>2. 等待定时任务执行<br>3. 查询 `sla_timer` 和通知表 |
| **预期结果** | `sla_timer.status=BREACHED`；`is_breached=1`；`breached_at` 有记录；通知表中有 `type=SLA_BREACHED` 记录；通知接收人包括工单处理人**和**分类默认处理组的组长 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

**模拟超时的 SQL（测试辅助）**：
```sql
UPDATE sla_timer
SET start_at = DATE_SUB(NOW(), INTERVAL 65 MINUTE)
WHERE ticket_id = ${TICKET_ID} AND timer_type = 'RESPONSE' AND status = 'RUNNING';
```

---

#### TC-BREACH-002：超时通知发送给处理人和组长

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-BREACH-002 |
| **用例名称** | SLA 超时通知同时发给处理人和所属分类默认处理组组长 |
| **前置条件** | 工单已分配给处理人；工单分类有默认处理组，组长存在 |
| **测试步骤** | 1. 触发 SLA BREACHED（参考 TC-BREACH-001）<br>2. 查询通知表，按 user_id 分组 |
| **预期结果** | 通知表中对该工单的 `SLA_BREACHED` 通知至少有 2 条，对应处理人和组长各一条 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-BREACH-003：SLA 预警级别颜色映射

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-BREACH-003 |
| **用例名称** | 不同剩余时间比例对应正确的 SLA 等级颜色 |
| **前置条件** | 存在 RUNNING 的 SLA 计时器 |
| **测试步骤** | 分别模拟以下 4 种 elapsed 情况：(1) elapsed=10%，(2) elapsed=55%，(3) elapsed=80%，(4) elapsed=100%+，查看接口或前端显示的颜色标识 |
| **预期结果** | (1) GREEN；(2) YELLOW；(3) ORANGE；(4) RED |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

### 2.5 看板统计（Dashboard）

#### TC-DASH-001：看板"SLA超时"统计数正确

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-DASH-001 |
| **用例名称** | 仪表盘概览行中 SLA超时数量与数据库一致 |
| **对应接口** | GET /api/dashboard/overview |
| **前置条件** | 数据库中存在 N 条 `sla_timer.is_breached=1` 且对应工单未关闭 |
| **测试步骤** | 1. 查询数据库中超时工单数 N<br>2. 调用 `/api/dashboard/overview`<br>3. 对比 `data.slaBreachedCount` |
| **预期结果** | `slaBreachedCount` = N；前端仪表盘 "SLA超时" 卡片数值与 N 一致 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

**验证 SQL**：
```sql
SELECT COUNT(DISTINCT ticket_id)
FROM sla_timer
WHERE is_breached = 1
  AND ticket_id IN (
    SELECT id FROM ticket WHERE status NOT IN ('completed','closed','rejected')
  );
```

---

#### TC-DASH-002：SLA 达成率计算正确

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-DASH-002 |
| **用例名称** | 仪表盘效率卡片中 SLA 达成率计算准确 |
| **对应接口** | GET /api/dashboard/sla-achievement（API000409） |
| **前置条件** | 存在已完成的工单，部分已超时，部分按时完成 |
| **测试步骤** | 1. 记录已完成工单总数 Total，其中未超时的数量 OK<br>2. 调用 `/api/dashboard/sla-achievement`<br>3. 对比 `data.achievementRate` |
| **预期结果** | `achievementRate` ≈ `(OK / Total) × 100`（允许小数误差 ±1%） |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-DASH-003：平均响应时间和解决时间统计

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-DASH-003 |
| **用例名称** | 仪表盘展示的平均响应/解决时间与数据库计算值一致 |
| **前置条件** | 存在多条已完成（COMPLETED）的 `sla_timer` 记录 |
| **测试步骤** | 1. 查询数据库计算平均 `elapsed_minutes`<br>2. 调用看板接口<br>3. 比较 `avgResponseMinutes` 和 `avgResolveMinutes` |
| **预期结果** | 差值在 ±1 分钟以内 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

### 2.6 通知中心

#### TC-NOTIF-001：分页查询通知列表

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-NOTIF-001 |
| **用例名称** | 分页查询当前用户通知列表 |
| **对应接口** | GET /api/notification/page（API000004） |
| **前置条件** | 当前用户有站内通知记录 |
| **测试步骤** | 1. 调用 `GET /api/notification/page?pageNum=1&pageSize=10` |
| **预期结果** | HTTP 200；返回分页结构，含 `records`、`total`、`pageNum`、`pageSize`；每条记录包含 `id`、`type`、`typeLabel`、`title`、`content`、`isRead` |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

```bash
curl -X GET "http://localhost:8080/api/notification/page?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer ${TOKEN}"
```

---

#### TC-NOTIF-002：按通知类型过滤

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-NOTIF-002 |
| **用例名称** | 按 SLA_WARNING 类型过滤通知 |
| **对应接口** | GET /api/notification/page（API000004） |
| **前置条件** | 存在 SLA_WARNING 和其他类型的通知 |
| **测试步骤** | 1. 调用 `GET /api/notification/page?type=SLA_WARNING` |
| **预期结果** | 返回的 `records` 中所有记录的 `type` 均为 `SLA_WARNING` |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-NOTIF-003：标记单条通知为已读

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-NOTIF-003 |
| **用例名称** | 将未读通知标记为已读 |
| **对应接口** | PUT /api/notification/read/{id}（API000005） |
| **前置条件** | 存在未读（`is_read=0`）通知，记录其 ID |
| **测试步骤** | 1. 调用 `PUT /api/notification/read/{id}`<br>2. 再次查询通知列表 |
| **预期结果** | 该通知 `is_read=1`，`read_at` 有值；未读计数减少 1 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-NOTIF-004：全部标记已读

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-NOTIF-004 |
| **用例名称** | 一键将所有未读通知标记为已读 |
| **对应接口** | PUT /api/notification/read/all（API000006） |
| **前置条件** | 存在多条未读通知 |
| **测试步骤** | 1. 调用 `PUT /api/notification/read/all`<br>2. 调用 `GET /api/notification/unread/count` |
| **预期结果** | `unreadCount = 0`；通知列表中所有 `is_read=1` |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-NOTIF-005：查询未读通知数量

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-NOTIF-005 |
| **用例名称** | 未读通知数量与实际未读数一致 |
| **对应接口** | GET /api/notification/unread/count（API000007） |
| **前置条件** | 数据库中当前用户有 N 条未读通知 |
| **测试步骤** | 1. 查询数据库未读数 N<br>2. 调用接口比较 `data.unreadCount` |
| **预期结果** | `unreadCount` = N |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-NOTIF-006：查询并更新通知偏好

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-NOTIF-006 |
| **用例名称** | 获取通知偏好并修改 SLA_WARNING 的渠道配置 |
| **对应接口** | GET /api/notification/preference（API000008）、PUT /api/notification/preference/update（API000009） |
| **前置条件** | 当前用户存在通知偏好记录 |
| **测试步骤** | 1. 调用 GET 接口获取偏好<br>2. 将 SLA_WARNING 的 `emailEnabled` 修改为 0<br>3. 调用 PUT 接口保存<br>4. 再次 GET 验证 |
| **预期结果** | 修改后 SLA_WARNING 的 `emailEnabled=0` 持久化成功 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-NOTIF-007：WebSocket 实时推送 SLA 预警

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-NOTIF-007 |
| **用例名称** | SLA 预警触发后，WebSocket 客户端实时收到推送 |
| **前置条件** | 浏览器已建立 `/ws/notification` 连接 |
| **测试步骤** | 1. 打开浏览器开发者工具 Network - WS<br>2. 触发 SLA 预警（参考 TC-WARN-001）<br>3. 观察 WebSocket 消息帧 |
| **预期结果** | 收到包含 `type=SLA_WARNING`、`ticketId`、`title`、`content` 的 JSON 消息帧；前端通知铃铛徽标数字自动更新 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

### 2.7 工单催办

#### TC-URGE-001：催办工单发送通知给处理人

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-URGE-001 |
| **用例名称** | 催办工单后处理人收到 URGE 类型通知 |
| **对应接口** | POST /api/ticket/urge/{id}（API000010） |
| **前置条件** | 工单已分配处理人，工单处于处理中状态 |
| **测试步骤** | 1. 使用工单创建人身份调用 `POST /api/ticket/urge/{ticketId}`<br>2. 查询通知表 |
| **预期结果** | 处理人收到 `type=URGE` 的站内通知；通知内容含工单信息 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

```bash
curl -X POST "http://localhost:8080/api/ticket/urge/${TICKET_ID}" \
  -H "Authorization: Bearer ${TOKEN}"
```

---

### 2.8 前端 UI 验证

#### TC-UI-001：SLA 管理页面正常加载

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-001 |
| **用例名称** | 访问 /manage/sla 页面正常显示策略列表 |
| **测试步骤** | 1. 登录系统<br>2. 点击左侧菜单"SLA管理"<br>3. 观察页面加载结果 |
| **预期结果** | 页面标题显示正确；表格呈现 4 条初始策略；各列（名称、优先级、响应时限、解决时限、预警阈值、状态）均有内容；无控制台报错 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-UI-002：关键字搜索过滤

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-002 |
| **用例名称** | 输入关键字后表格内容实时过滤 |
| **测试步骤** | 1. 在搜索框输入"紧急"<br>2. 观察表格结果 |
| **预期结果** | 只显示名称或描述含"紧急"的策略行 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-UI-003：按优先级过滤

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-003 |
| **用例名称** | 选择"高"优先级过滤后只展示 HIGH 策略 |
| **测试步骤** | 1. 在优先级下拉框选择"高"<br>2. 观察表格 |
| **预期结果** | 只有 priority=HIGH 的策略行显示 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-UI-004：按状态过滤（启用/禁用）

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-004 |
| **用例名称** | 过滤"已禁用"策略 |
| **测试步骤** | 1. 先禁用一条策略（TC-SLA-006）<br>2. 在状态筛选下拉选"已禁用" |
| **预期结果** | 只显示 `is_active=0` 的策略行 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-UI-005：创建 SLA 策略对话框表单校验

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-005 |
| **用例名称** | 创建对话框中不填写必填字段时显示校验提示 |
| **测试步骤** | 1. 点击"新增"按钮<br>2. 不填任何字段，直接点击"确认"<br>3. 观察表单提示 |
| **预期结果** | 策略名称、优先级、响应时限、解决时限字段下方显示"不能为空"类提示；不发出网络请求 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-UI-006：编辑 SLA 策略对话框预填值

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-006 |
| **用例名称** | 点击编辑时对话框自动回填当前策略数据 |
| **测试步骤** | 1. 在策略列表某行点击"编辑"<br>2. 观察弹出对话框各字段值 |
| **预期结果** | 对话框中的名称、优先级、响应时限、解决时限、预警阈值、描述均与列表行一致 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-UI-007：启用/禁用开关实时生效

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-007 |
| **用例名称** | 点击表格行中的启用开关后策略状态立即切换 |
| **测试步骤** | 1. 在列表中找到启用的策略<br>2. 点击开关切换为禁用<br>3. 观察开关状态和网络请求 |
| **预期结果** | 开关变为关闭状态；Network 面板中发出 `PUT /api/sla/policy/update` 请求且状态 200；页面不刷新；数据库中 `is_active=0` |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-UI-008：仪表盘 SLA 超时卡片

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-008 |
| **用例名称** | 仪表盘概览区 SLA 超时数字正确显示 |
| **测试步骤** | 1. 访问仪表盘主页<br>2. 观察"SLA超时"卡片数字<br>3. 触发一次 SLA 超时<br>4. 刷新页面，再次观察 |
| **预期结果** | 初始数字与数据库一致；超时触发后数字+1 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-UI-009：仪表盘 SLA 达成率圆形进度

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-UI-009 |
| **用例名称** | 仪表盘效率卡片中 SLA 达成率圆形进度组件显示正确 |
| **测试步骤** | 1. 访问仪表盘<br>2. 查看效率卡片中的"SLA达成率"圆形进度图 |
| **预期结果** | 圆形进度值与接口返回的百分比一致；颜色随达成率变化（≥90%绿色，60-90%黄色，<60%红色或类似） |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

### 2.9 集成与端到端测试

#### TC-E2E-001：工单完整 SLA 生命周期（正常路径）

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-E2E-001 |
| **用例名称** | 从工单创建到解决的完整 SLA 周期验证 |
| **测试步骤** | 1. 创建工单（分类绑定 HIGH 优先级 SLA，响应30min，解决240min）<br>2. 验证 RESPONSE + RESOLVE timer 均 RUNNING（TC-TIMER-001）<br>3. 处理人响应（流转到 processing），验证 RESPONSE timer COMPLETED<br>4. 处理人挂起工单，验证 timer PAUSED（TC-TIMER-002）<br>5. 恢复处理，验证 timer RUNNING（TC-TIMER-003）<br>6. 解决工单，验证所有 timer COMPLETED（TC-TIMER-005）<br>7. 验证仪表盘 SLA 达成率提升 |
| **预期结果** | 所有步骤均符合预期；全流程无异常；SLA 统计数据更新正确 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-E2E-002：工单完整 SLA 生命周期（超时路径）

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-E2E-002 |
| **用例名称** | 工单未及时处理导致 SLA 超时的完整流程 |
| **测试步骤** | 1. 创建工单（分类绑定 URGENT 优先级 SLA，响应15min）<br>2. 不进行任何处理，等待/模拟计时器超时<br>3. 触发定时任务扫描<br>4. 查看通知、仪表盘、timer 状态 |
| **预期结果** | is_warned=1；随后 is_breached=1，status=BREACHED；处理人和组长均收到 SLA_BREACHED 通知；仪表盘 SLA 超时数 +1；SLA 达成率下降 |
| **优先级** | P0 |
| **执行状态** | ⬜ 待执行 |

---

#### TC-E2E-003：分类绑定 SLA 策略影响新建工单

| 字段 | 内容 |
|------|------|
| **用例编号** | TC-E2E-003 |
| **用例名称** | 修改工单分类绑定的 SLA 策略后，新建工单使用新策略 |
| **测试步骤** | 1. 将某工单分类的 sla_policy_id 从策略 A 改为策略 B<br>2. 在该分类下创建新工单<br>3. 查询 `sla_timer` 的 `sla_policy_id` 和 `threshold_minutes` |
| **预期结果** | 新工单的 timer 使用策略 B 的阈值时间 |
| **优先级** | P1 |
| **执行状态** | ⬜ 待执行 |

---

## 三、测试用例汇总

| 模块 | 用例编号 | 优先级 | 说明 |
|------|---------|--------|------|
| SLA策略管理 | TC-SLA-001 ~ TC-SLA-008 | P0/P1 | CRUD + 参数校验 |
| SLA计时器 | TC-TIMER-001 ~ TC-TIMER-005 | P0 | 启动/暂停/续计/完成 |
| 工作时间计算 | TC-WTIME-001 ~ TC-WTIME-003 | P0 | 当日/跨天/跨周末 |
| 预警与超时 | TC-WARN-001 ~ TC-BREACH-003 | P0/P1 | 触发/幂等/颜色 |
| 看板统计 | TC-DASH-001 ~ TC-DASH-003 | P0/P1 | 超时数/达成率/均值 |
| 通知中心 | TC-NOTIF-001 ~ TC-NOTIF-007 | P0/P1 | 列表/已读/偏好/WS |
| 工单催办 | TC-URGE-001 | P1 | 催办通知 |
| 前端UI | TC-UI-001 ~ TC-UI-009 | P0/P1 | 页面/过滤/表单/看板 |
| 端到端 | TC-E2E-001 ~ TC-E2E-003 | P0/P1 | 正常/超时/分类绑定 |

**P0 用例数量**：24  
**P1 用例数量**：15  
**总计**：39 条

---

## 四、测试数据准备

### 4.1 获取 JWT Token

```bash
# 参考 AGENTS.md 中的 JWT secret
TOKEN=$(python3 -c "
import jwt, time
payload = {
  'sub': '1',
  'userId': 1,
  'username': 'test_admin',
  'iat': int(time.time()),
  'exp': int(time.time()) + 86400
}
print(jwt.encode(payload, 'miduo-ticket-platform-jwt-secret-key-2026-must-be-long-enough', algorithm='HS256'))
")
echo "TOKEN=$TOKEN"
```

### 4.2 确认初始 SLA 策略

```sql
SELECT id, name, priority, response_time, resolve_time, warning_pct, is_active
FROM sla_policy
ORDER BY id;
```

### 4.3 查询工单分类与 SLA 绑定关系

```sql
SELECT id, name, sla_policy_id
FROM ticket_category
WHERE deleted = 0;
```

### 4.4 查询 SLA 计时器状态

```sql
SELECT t.id, t.ticket_id, t.timer_type, t.status,
       t.threshold_minutes, t.elapsed_minutes,
       t.start_at, t.deadline, t.is_warned, t.is_breached
FROM sla_timer t
WHERE t.deleted = 0
ORDER BY t.ticket_id, t.timer_type;
```

### 4.5 快速模拟 SLA 预警触发

```sql
-- 让某工单的 RESPONSE timer 消耗 80% 时间（超过 warningPct=75）
-- 假设 threshold=30min，elapsed 需 >= 23min
UPDATE sla_timer
SET start_at = DATE_SUB(NOW(), INTERVAL 25 MINUTE),
    elapsed_minutes = 0
WHERE ticket_id = ${TICKET_ID}
  AND timer_type = 'RESPONSE'
  AND status = 'RUNNING';
```

### 4.6 快速模拟 SLA 超时触发

```sql
-- 让某工单的 RESPONSE timer 超时（elapsed > threshold）
-- 假设 threshold=30min
UPDATE sla_timer
SET start_at = DATE_SUB(NOW(), INTERVAL 35 MINUTE),
    elapsed_minutes = 0
WHERE ticket_id = ${TICKET_ID}
  AND timer_type = 'RESPONSE'
  AND status = 'RUNNING';
```

---

## 五、已知测试注意事项

1. **工作时间限制**：TC-WTIME 系列用例中，若当前时间在非工作时间段内，应在工作日工作时间内执行测试，避免 deadline 计算结果不直观。
2. **定时任务间隔**：SlaCheckJobHandler 每 60 秒执行一次，模拟超时/预警后最长需等待 60 秒才能看到效果。
3. **消息聚合**：通知编排器通过 Redis 实现 5 分钟内相同类型通知合并，短时间内重复触发同类型通知可能只收到一条聚合通知，这是正常行为。
4. **WebSocket 鉴权**：WS 连接时需携带 `userId` 参数，确保与当前登录用户一致。
5. **Docker 依赖**：执行前确认 Docker daemon 已启动并且 MySQL/Redis 容器均在运行状态。

---

## 六、缺陷记录模板

执行测试用例时，若发现问题，使用以下格式记录：

| 字段 | 内容 |
|------|------|
| **缺陷编号** | BUG-SLA-XXX |
| **关联用例** | TC-SLA-XXX |
| **严重级别** | 致命 / 严重 / 一般 / 建议 |
| **标题** | 简短描述问题 |
| **重现步骤** | 逐步操作 |
| **实际结果** | 观察到的结果 |
| **预期结果** | 期望的正确结果 |
| **截图/日志** | 附件 |
| **发现人** | 测试人员名称 |
| **发现时间** | 日期 |
