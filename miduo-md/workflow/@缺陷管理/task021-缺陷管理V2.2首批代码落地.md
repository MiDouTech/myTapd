# task021-缺陷管理V2.2首批代码落地

> 日期：2026-03-17  
> 状态：🚧进行中  
> 关联文档：`task019-技术缺陷管理规范优化-研发排期确认版PRD.md`、`task020-技术缺陷管理规范优化-开发启动包.md`

---

## 1. 本次目标

按 V2.2 规范先完成第一批“可运行底座”改造，重点解决以下问题：

1. 缺陷等级口径历史兼容（FATAL/CRITICAL/NORMAL/MINOR → P0~P4）
2. 缺陷状态体系补齐（新增 `investigating`、`temp_resolved`）
3. 前后端状态/等级展示与编辑口径统一

---

## 2. 已完成改造清单（首批）

## 2.1 数据库迁移（Flyway）

- 新增迁移脚本：  
  `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V25__defect_management_v22_alignment.sql`

### 迁移内容

1. `ticket_bug_test_info.severity_level` 历史值映射：
   - `FATAL` → `P0`
   - `CRITICAL` → `P1`
   - `NORMAL` → `P2`
   - `MINOR` → `P3`
2. 统一 `P0~P4` 大小写并更新字段注释口径
3. 更新缺陷工作流（`workflow.id=3`）状态与流转，补齐：
   - `investigating`（排查中）
   - `temp_resolved`（临时解决）

---

## 2.2 后端代码

### 已修改

1. `ticket-common/.../TicketStatus.java`
   - 新增状态枚举：
     - `INVESTIGATING("investigating", "排查中")`
     - `TEMP_RESOLVED("temp_resolved", "临时解决")`

2. `ticket-application/.../ticket/TicketBugApplicationService.java`
   - 编辑权限状态判断改为标准小写状态码口径
   - 新增缺陷等级标准化逻辑：
     - 兼容历史值（FATAL/CRITICAL/NORMAL/MINOR）
     - 统一入库为 `P0~P4`
     - 非法等级直接参数错误拦截

3. `ticket-application/.../ticket/TicketChangeHistoryRecorder.java`
   - 变更历史里的 severity 对比增加历史值兼容标准化，避免同义等级误判为变更

4. `ticket-application/.../ticket/KanbanApplicationService.java`
   - 看板状态排序补齐 `investigating`、`temp_resolved`

---

## 2.3 前端代码

### 已修改

1. `miduo-frontend/src/views/ticket/components/bug/BugStatusBadge.vue`
   - 新增状态展示：
     - `investigating`（排查中）
     - `temp_resolved`（临时解决）

2. `miduo-frontend/src/views/ticket/components/bug/BugDetailInfoPanel.vue`
   - 缺陷等级显示统一为 `P0~P4`
   - 保留历史值兼容映射展示（FATAL/CRITICAL/NORMAL/MINOR）

3. `miduo-frontend/src/views/ticket/TicketDetailView.vue`
   - 缺陷详情页状态判断改为标准小写状态码
   - 测试信息“缺陷等级”下拉改为 `P0~P4`
   - 状态文案映射补齐 `investigating`、`temp_resolved`

4. `miduo-frontend/src/views/ticket/TicketPublicView.vue`
   - 公开页状态颜色映射补齐缺陷状态全集

---

## 3. 接口影响说明

本轮未新增 Controller 接口，属于“存量接口口径对齐”：

- 入参：`TicketBugTestInfoInput.severityLevel` 统一接受 `P0~P4`，并兼容历史值
- 出参：缺陷等级展示口径向 `P0~P4` 收敛

> 接口编号：本轮无新增 API 编号。

---

## 4. 风险与注意事项

1. 迁移后，依赖旧等级值（FATAL 等）的下游统计若未兼容会出现口径偏差，需联动验证报表口径。
2. 工作流状态新增后，若外部系统有状态白名单，需要同步更新。
3. 前端兼容了旧值展示，但建议尽快清理历史旧口径数据，避免长期双制。

---

## 5. 下一步（task022建议）

1. 接入 `DefectWorkflowGuardService`，补齐“临时解决必填方案/挂起必填原因”等强校验。
2. 对接 SLA 生命周期动作（start/pause/resume/complete）到新增状态流转。
3. 开始 `task009~task012` 的时限承诺、外部溯源凭证、改进任务闭环。

