-- V24: 删除操作日志表中的所属应用字段
-- 工单系统作为独立系统，无需区分所属应用，app_code 和 app_name 字段无实际意义

ALTER TABLE sys_operation_log
    DROP COLUMN IF EXISTS app_code,
    DROP COLUMN IF EXISTS app_name;
