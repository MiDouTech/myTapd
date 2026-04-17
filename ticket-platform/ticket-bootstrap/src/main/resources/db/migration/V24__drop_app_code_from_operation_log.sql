-- V24: 删除操作日志表中的所属应用字段（若仍存在）
-- 工单系统作为独立系统，无需区分所属应用，app_code 和 app_name 字段无实际意义。
--
-- 为何要做“存在才删”：部分环境表由 V29 或手工脚本建成最终结构，本身没有这两列；
-- V22 改为 IF NOT EXISTS 后，V22/V23 不会改表结构，直接执行 DROP 会触发 MySQL 1091。
-- MySQL 8.0 不支持 DROP COLUMN IF EXISTS（MariaDB 扩展），故用 information_schema 判断。

SET @db := DATABASE();

SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'sys_operation_log'
        AND COLUMN_NAME = 'app_code'
    ),
    'ALTER TABLE `sys_operation_log` DROP COLUMN `app_code`',
    'SELECT 1'
  )
);
PREPARE stmt_drop_app_code FROM @stmt;
EXECUTE stmt_drop_app_code;
DEALLOCATE PREPARE stmt_drop_app_code;

SET @stmt := (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db
        AND TABLE_NAME = 'sys_operation_log'
        AND COLUMN_NAME = 'app_name'
    ),
    'ALTER TABLE `sys_operation_log` DROP COLUMN `app_name`',
    'SELECT 1'
  )
);
PREPARE stmt_drop_app_name FROM @stmt;
EXECUTE stmt_drop_app_name;
DEALLOCATE PREPARE stmt_drop_app_name;
