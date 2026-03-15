# Task007-组织账号管理技术实现方案

## 1. 目标

在不破坏现有工单业务能力的前提下，基于既有 `API000425~API000430` 组织与同步接口，完成“组织账号管理”页面重构与后端数据增强，确保：

1. 前端页面具备可用的组织查询与同步运维能力；
2. 后端支持性别字段与部门成员统计输出；
3. 数据库可通过 Flyway 平滑升级。

---

## 2. 总体设计

## 2.1 分层职责

1. `ticket-controller`：复用现有组织查询与同步接口；
2. `ticket-application`：
   - `DepartmentApplicationService` 负责部门树 + 成员统计聚合；
   - `UserApplicationService` 负责成员分页筛选与详情输出；
   - `WecomSyncService` 负责企微数据同步（含性别映射）。
3. `ticket-infrastructure`：
   - `WecomClient` 解析企微用户性别字段；
   - `RepositoryImpl + PO` 完成字段持久化映射。
4. `miduo-frontend`：
   - 新增 `organization.ts` API 封装与类型定义；
   - 重构 `UserManageView.vue` 实现组织账号管理页面。

## 2.2 接口复用清单

1. `POST /api/v1/sync/manual`（API000425）
2. `GET /api/v1/sync/status`（API000426）
3. `GET /api/v1/sync/log/page`（API000427）
4. `GET /api/v1/departments/tree`（API000428）
5. `GET /api/v1/employees/page`（API000429）
6. `GET /api/v1/employees/detail/{id}`（API000430）

---

## 3. 数据模型与字段设计

## 3.1 数据库变更（Flyway V12）

新增脚本：

- `ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V12__enhance_org_account_management.sql`

变更内容：

1. `sys_user` 新增字段：
   - `gender` tinyint(4) default 0，含义 `0未知/1男/2女`
2. `sys_user` 新增组合索引：
   - `idx_department_status_deleted (department_id, account_status, deleted)`

## 3.2 Java模型扩展

1. `User` / `SysUserPO` 增加：
   - `gender`、`syncStatus`、`syncTime`
2. `Department` / `DepartmentPO` 增加：
   - `deptStatus`、`syncStatus`、`syncTime`、`leaderWecomUserid`
3. DTO增强：
   - `DepartmentTreeOutput`：增加 `directUserCount`、`totalUserCount`、状态字段；
   - `EmployeePageOutput`、`EmployeeDetailOutput`：增加 `gender`、`genderName`；
   - `EmployeePageInput`：增加 `gender` 查询条件。

---

## 4. 核心实现说明

## 4.1 部门树成员统计

在 `DepartmentApplicationService` 中：

1. 读取全部部门；
2. 读取全部在职用户并按 `departmentId` 聚合直属人数；
3. 递归构建树并计算 `totalUserCount`（直属 + 子部门汇总）。

## 4.2 员工查询增强

在 `UserApplicationService` 中：

1. 员工分页新增性别筛选；
2. 分页与详情结果补充 `genderName` 输出；
3. 保持敏感字段脱敏策略不变。

## 4.3 企微字段映射

在 `WecomClient` 与 `WecomSyncService` 中：

1. 从企微成员数据解析 `gender`；
2. 同步新增/更新用户时写入 `gender`；
3. 状态补充兼容：企微 `status=5` 归并为本地离职态（4）。

---

## 5. 前端实现说明

## 5.1 新增类型与API

1. `src/types/organization.ts`
2. `src/api/organization.ts`

实现组织查询、详情、同步状态、同步日志的统一封装。

## 5.2 页面重构

重构 `src/views/manage/UserManageView.vue`，包含：

1. 顶部筛选与同步工具栏；
2. 左侧部门树（成员数 + 状态）；
3. 右侧成员列表（分页、排序、详情抽屉）；
4. 同步日志弹窗（分页查询）。

并将菜单与路由标题统一为“组织账号管理”。

---

## 6. 风险与对策

1. **旧数据缺少 gender**
   - 对策：默认输出“未知”，不影响页面可用性。
2. **同步失败导致状态不一致**
   - 对策：保留同步日志查询入口，按失败信息排障后可手动重试。
3. **大组织数据量下查询性能**
   - 对策：增加组合索引，并保持后端分页查询。

---

## 7. 验证计划

1. 后端编译：`mvn -pl ticket-controller -am -DskipTests compile`（JDK8）
2. 前端构建：`npm run build`
3. 核验项：
   - 部门树统计与成员列表联动；
   - 同步按钮可触发且状态刷新；
   - 同步日志分页可查询；
   - 性别字段在列表与详情均可展示。
