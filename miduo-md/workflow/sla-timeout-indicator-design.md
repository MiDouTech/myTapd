# SLA超时标记优化 — 工单列表

## 变更背景

当前工单列表接口 (`GET /api/ticket/page`) 返回的 `TicketListOutput` 不包含任何 SLA 相关信息。  
SLA 计时器数据存在于 `sla_timer` 表中，包含响应超时和解决超时两种类型的计时器状态（RUNNING / PAUSED / COMPLETED / BREACHED）。  
用户在工单列表中无法直接感知哪些工单已经 SLA 超时或即将超时，需要逐一进入详情才能了解 SLA 情况。

## 现状问题

1. 工单列表无 SLA 超时视觉提示，用户无法快速识别紧急工单
2. SLA 超时信息需打开工单详情或查看仪表盘才能获知，效率低下

## 目标

- 在工单列表每行中展示 SLA 超时标记，让用户一眼识别超时工单
- 不改变现有 SLA 计算逻辑（超时判定仍由 `SlaCheckJobHandler` 定时任务负责）
- 不改变对外 API 契约的语义（向后兼容：新增字段，已有字段不变）

## 非目标

- 不改变 SLA 策略管理（增删改查）
- 不改变 SLA 计时器的运行/暂停/完成/超时逻辑
- 不做实时倒计时（列表仅展示状态快照）
- 不做 SLA 相关的列排序或筛选（本次不涉及）

## 方案概要

### 后端

1. **`TicketListOutput` 新增字段：**
   - `slaStatus`（String）：SLA 整体状态。取值 `NORMAL`（正常）、`WARNING`（预警）、`BREACHED`（已超时）、`null`（无 SLA 策略绑定）
   - `slaStatusLabel`（String）：SLA 状态中文标签
2. **查询方式：** 在 `getTicketPage` 方法中，根据本页工单 ID 批量查询 `sla_timer` 表，汇总每张工单的最严重 SLA 状态。避免循环查询。
3. **逻辑：** 对每张工单，取其所有未删除的 `sla_timer` 记录，按 BREACHED > WARNING(is_warned=1) > RUNNING/PAUSED/COMPLETED 取最严重级别。

### 前端

1. 工单列表表格中在「状态」列后新增「SLA」列
2. 使用 `el-tag` 展示 SLA 状态标记：
   - `BREACHED`（已超时）→ 红色 danger 标签
   - `WARNING`（预警中）→ 橙色 warning 标签
   - `NORMAL`（正常）→ 不显示标签（或显示绿色标签"正常"）
   - 无 SLA → 显示 `-`

### 影响范围

- **后端改动文件：**
  - `TicketListOutput.java` — 新增 `slaStatus` / `slaStatusLabel`
  - `TicketApplicationService.java` — `getTicketPage` 中批量查询 `sla_timer`，填充 SLA 状态
  - `SlaTimerMapper.java` — 新增批量按 ticketIds 查询方法
  - `SlaTimerMapper.xml` — 新增批量查询 SQL
- **前端改动文件：**
  - `ticket.ts` — `TicketListOutput` 类型新增 `slaStatus` / `slaStatusLabel`
  - `TicketListView.vue` — 表格新增 SLA 列

### 关键决策

- 采用"批量查询 + 内存聚合"策略，不改变分页 SQL，避免复杂 JOIN
- SLA 状态由后端计算，前端仅展示，确保单一数据源
- 无 SLA 绑定的工单（无 sla_timer 记录）不显示任何标记

### 回滚策略

- 前端可通过移除 SLA 列恢复
- 后端新增字段为可选字段，移除不影响已有消费方

## 验收标准

- [x] 工单列表接口返回 `slaStatus` 和 `slaStatusLabel` 字段
- [x] 超时工单在列表中显示红色"已超时"标签
- [x] 预警工单显示橙色"预警中"标签
- [x] 无 SLA 工单显示 `-`
- [x] 前后端编译通过
