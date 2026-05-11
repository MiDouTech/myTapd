# task032-补齐START_PROCESS缺失节点脚本

## 1. 背景

在历史工单里，存在这类现象：

- 节点列表中有“待测试受理/待开发受理”的离开时间
- 但缺少“测试复现中/开发解决中”节点行

原因是旧逻辑把 `START_PROCESS` 当“仅开始处理打点”，没有创建目标节点行。

## 2. 目标

基于历史 `ticket_time_track` 的 `START_PROCESS(from_status != to_status)` 事件，补齐缺失目标节点：

1. 插入缺失节点行（如 `TESTING`、`DEVELOPING`）
2. 补齐 `arrive_at / start_process_at / leave_at`
3. 同步回算 `wait/process/total` 耗时

## 3. 脚本位置

- `miduo-md/database/20260511_2_backfill_missing_start_process_nodes.sql`

## 4. 执行顺序（建议）

1. 先执行：
   - `20260511_1_backfill_ticket_node_duration_leave_at.sql`
2. 再执行：
   - `20260511_2_backfill_missing_start_process_nodes.sql`
3. 执行后在工单详情“节点耗时统计”抽样核对：
   - 是否出现“测试复现中/开发解决中”节点
   - 时间顺序是否连续

## 5. 核心策略

1. 候选来源：`ticket_time_track` 中动作为 `START_PROCESS` 且 `from_status != to_status` 的事件
2. 去重条件：若目标节点在同工单、同状态、同时间窗口（60秒）已存在，则不重复插入
3. 时间回填规则：
   - `arrive_at = START_PROCESS.timestamp`
   - `start_process_at = arrive_at`
   - `leave_at = min(下一节点到达时间, 该状态下一次流转离开时间)`

## 6. 风险与边界

1. 极少数历史脏数据（时间乱序）会保守处理，避免错误插入
2. 当前进行中的最后节点若无离开依据，`leave_at` 允许为空（符合业务语义）
3. 建议先在测试库执行并核对 `remain_missing_start_process_target_node`
