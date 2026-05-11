# task031-节点耗时历史数据回填脚本

## 1. 背景

在历史数据中，部分工单节点存在 `leave_at` 为空的问题。典型场景是：

- `待测试受理 -> 测试复现中`
- `待开发受理 -> 开发解决中`

旧链路把这类流转记成 `START_PROCESS`，只记录了“开始处理”，未关闭前一节点，导致节点耗时断链。

## 2. 目标

一次性回填历史数据，确保已离开的节点具备完整时间字段：

1. `leave_at`
2. `start_process_at`（缺失时补齐）
3. `wait_duration_sec / process_duration_sec / total_duration_sec`

## 3. 方案概要

回填脚本文件：

- `miduo-md/database/20260511_1_backfill_ticket_node_duration_leave_at.sql`

核心策略：

1. 为每条 `leave_at IS NULL` 的节点记录计算候选离开时间：
   - 优先用“下一节点 `arrive_at`”
   - 同时参考 `ticket_time_track` 中对应 `from_status` 的流转时间
   - 两者取更早值
2. 仅回填可算出候选离开时间的历史节点，避免误更新当前进行中的最后一个节点
3. 同步补齐耗时字段，保证统计闭环

## 4. 执行步骤

1. 在测试环境先执行脚本，确认预览结果与受影响行数
2. 在生产低峰执行脚本
3. 执行后做校验：
   - `remain_open_with_next_node` 应明显下降
   - `closed_node_without_start_process` 应明显下降

示例执行命令：

```bash
mysql -h <host> -P <port> -u <user> -p<password> <database> \
  < /path/to/miduo-md/database/20260511_1_backfill_ticket_node_duration_leave_at.sql
```

## 5. 回滚策略

脚本默认在事务中执行并 `COMMIT`。如需演练回滚：

1. 将脚本末尾 `COMMIT` 改为 `ROLLBACK`
2. 在测试环境先验证预览和更新影响范围
3. 生产执行前务必先备份 `ticket_node_duration` 表

## 6. 风险与边界

1. 历史特别脏的数据（如节点乱序、同时多开节点）会保守回填，不会强行猜测当前节点
2. 当前进行中的最后节点若无下一节点/无流转事件，不会被写 `leave_at`（符合业务语义）
3. 建议与工单时间链界面抽样联查，确认关键工单显示正确
