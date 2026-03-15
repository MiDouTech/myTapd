# Task002-数据模型与字段映射说明

## 1. 目标

为企业微信账号体系复用提供一期数据库基座，满足配置、同步、查询、认证与派单的后续开发依赖。

## 2. 已落地脚本

- Flyway脚本：`ticket-platform/ticket-bootstrap/src/main/resources/db/migration/V10__init_wework_identity_reuse.sql`

## 3. 模型落地策略说明

任务设计文档中建议表名为 `sys_department`、`sys_employee`，当前项目已存在并稳定使用：

1. `department`（等价于 `sys_department`）
2. `sys_user`（等价于 `sys_employee`）

为降低改造风险，本次采用**兼容增强**策略：在现有表上补字段与约束，而非直接重命名主表。

## 4. 本次新增/增强对象

### 4.1 增强现有表

1. `department`
   - 新增：`dept_status`、`sync_status`、`sync_time`、`leader_wecom_userid`
   - 约束：新增唯一索引 `uk_department_wecom_dept_id(wecom_dept_id)`
2. `sys_user`
   - 新增：`sync_status`、`sync_time`
3. `ticket`
   - 新增：`creator_wework_userid`、`assignee_wework_userid`、`current_dept_id`
   - 索引：三项新增字段索引

### 4.2 新建表

1. `sys_wework_config`：企微连接配置与同步参数
2. `sys_sync_log`：同步执行日志（结果、重试、耗时、错误原因）
3. `ticket_user_role`：`wework_userid + role_code` 本地授权映射
4. `ticket_assignment_rule`：自动派单与兜底规则

## 5. 字段映射摘要（企微 -> 本地）

### 5.1 部门

| 企微字段 | 本地字段 | 说明 |
|---|---|---|
| id | department.wecom_dept_id | 外部唯一标识 |
| name | department.name | 部门名称 |
| parentid | department.parent_id | 父部门本地ID（同步时换算） |
| order | department.sort_order | 展示排序 |
| department_leader | department.leader_wecom_userid | 部门负责人 |

### 5.2 员工

| 企微字段 | 本地字段 | 说明 |
|---|---|---|
| userid | sys_user.wecom_userid | 外部唯一标识 |
| name | sys_user.name | 员工姓名 |
| mobile | sys_user.phone | 手机 |
| email | sys_user.email | 邮箱 |
| position | sys_user.position | 职位 |
| department[0] / main_department | sys_user.department_id | 主部门本地ID |
| status | sys_user.account_status | 状态映射（1在职/2停用/4未激活） |

## 6. 回滚脚本建议（手工执行）

> 说明：生产环境请先评估数据影响，再执行回滚SQL。

```sql
-- 1) 关闭定时任务与新功能入口（应用配置层）
-- scheduleEnabled=false

-- 2) 删除新建表（如需完全回滚）
DROP TABLE IF EXISTS `ticket_assignment_rule`;
DROP TABLE IF EXISTS `ticket_user_role`;
DROP TABLE IF EXISTS `sys_sync_log`;
DROP TABLE IF EXISTS `sys_wework_config`;

-- 3) 回退 ticket 新增字段
ALTER TABLE `ticket`
    DROP INDEX `idx_ticket_creator_wework_userid`,
    DROP INDEX `idx_ticket_assignee_wework_userid`,
    DROP INDEX `idx_ticket_current_dept_id`,
    DROP COLUMN `creator_wework_userid`,
    DROP COLUMN `assignee_wework_userid`,
    DROP COLUMN `current_dept_id`;

-- 4) 回退 sys_user 新增字段
ALTER TABLE `sys_user`
    DROP COLUMN `sync_status`,
    DROP COLUMN `sync_time`;

-- 5) 回退 department 新增字段与唯一约束
ALTER TABLE `department`
    DROP INDEX `uk_department_wecom_dept_id`,
    DROP COLUMN `dept_status`,
    DROP COLUMN `sync_status`,
    DROP COLUMN `sync_time`,
    DROP COLUMN `leader_wecom_userid`;
```

## 7. 下一步（Task003准备）

1. 基于 `sys_wework_config` 接入配置读写接口与脱敏展示。
2. 落地 `WeworkApiClient` 与 token 缓存。
3. 提供连接测试接口并标准化错误码返回。
