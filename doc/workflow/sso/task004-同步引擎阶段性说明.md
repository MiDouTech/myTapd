# Task004-同步引擎阶段性说明

## 1. 本阶段完成项

1. 手动同步接口
   - `POST /api/v1/sync/manual`（`API000425`）
2. 最近同步状态接口
   - `GET /api/v1/sync/status`（`API000426`）
3. 同步日志分页接口
   - `GET /api/v1/sync/log/page`（`API000427`）
4. 同步日志落库
   - 新增 `SysSyncLogPO`、`SysSyncLogMapper`
   - 手动与定时同步统一写入 `sys_sync_log`
5. 定时任务动态调度接入
   - `WecomSyncJob` 改为固定间隔轮询配置，并根据 `scheduleEnabled/scheduleCron` 动态触发
6. 同步失败重试能力
   - 从 `sys_wework_config.retry_count` 读取重试次数
   - 对异常场景执行递增退避重试，并将最终重试次数落库到 `sys_sync_log.retry_count`

## 2. 同步流程说明

1. 同步顺序固定：先部门、后员工。
2. 手动触发时返回：总数、成功数、失败数、错误信息、耗时。
3. 定时触发时不对前端返回，但会写日志用于追踪。
4. 同步状态定义：
   - `SUCCESS`：失败数为0
   - `PARTIAL`：存在部分失败
   - `FAILED`：同步过程抛出异常中断

## 3. 当前限制（下一步优化）

1. 当前按“单次全量同步”执行，重试次数字段先记录为0。
2. 员工同步仍按现有逻辑逐条upsert，后续可优化为批量读取+内存映射。

## 4. 编译验证

- 执行：`mvn -pl ticket-job -am -DskipTests compile`
- 结果：`BUILD SUCCESS`
