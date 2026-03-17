-- V24: 删除操作日志表中的所属应用字段
-- 工单系统作为独立系统，无需区分所属应用，app_code 和 app_name 字段无实际意义
-- 注意：DROP COLUMN IF EXISTS 是 MariaDB 扩展语法，MySQL 8.0 不支持；
--       本迁移在 V22/V23 之后执行，两列一定存在，无需 IF EXISTS 保护。

ALTER TABLE sys_operation_log
    DROP COLUMN app_code,
    DROP COLUMN app_name;
