-- V24: 删除操作日志表中的所属应用字段
-- 工单系统作为独立系统，无需区分所属应用，app_code 和 app_name 字段无实际意义
--
-- V22/V23 使用 CREATE TABLE IF NOT EXISTS：若表已由其它脚本创建且不含上述列，
-- 直接 DROP 会在 MySQL 8 上报错 1091。这里按列是否存在分别执行，避免启动失败。
-- （MySQL 8.0 不支持 DROP COLUMN IF EXISTS，故用 information_schema + 预处理语句。）

SET @schema_name = DATABASE();

-- app_code（若存在会一并移除仅引用该列的索引，如 idx_app_code）
SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_operation_log'
      AND COLUMN_NAME = 'app_code'
);
SET @ddl = IF(@col_exists > 0,
    'ALTER TABLE sys_operation_log DROP COLUMN app_code',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_operation_log'
      AND COLUMN_NAME = 'app_name'
);
SET @ddl = IF(@col_exists > 0,
    'ALTER TABLE sys_operation_log DROP COLUMN app_name',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
