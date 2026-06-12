-- ============================================================
-- V62__workflow_customization_delete_guard.sql
-- 工作流自定义配置与安全删除增强
-- 1. 新增工作流调用统计字段
-- 2. 回填历史调用数据
-- ============================================================

SET @col = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'workflow'
      AND COLUMN_NAME = 'invocation_count'
);
SET @sql = IF(
    @col = 0,
    'ALTER TABLE `workflow` ADD COLUMN `invocation_count` bigint(20) NOT NULL DEFAULT 0 COMMENT ''被工单调用次数'' AFTER `is_active`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'workflow'
      AND COLUMN_NAME = 'first_invoked_time'
);
SET @sql = IF(
    @col = 0,
    'ALTER TABLE `workflow` ADD COLUMN `first_invoked_time` datetime DEFAULT NULL COMMENT ''首次被调用时间'' AFTER `invocation_count`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'workflow'
      AND COLUMN_NAME = 'last_invoked_time'
);
SET @sql = IF(
    @col = 0,
    'ALTER TABLE `workflow` ADD COLUMN `last_invoked_time` datetime DEFAULT NULL COMMENT ''最近一次被调用时间'' AFTER `first_invoked_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'workflow'
      AND INDEX_NAME = 'idx_invocation_count'
);
SET @sql = IF(
    @idx = 0,
    'ALTER TABLE `workflow` ADD KEY `idx_invocation_count` (`invocation_count`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `workflow` w
LEFT JOIN (
    SELECT
        `workflow_id`,
        COUNT(1) AS `cnt`,
        MIN(`create_time`) AS `first_time`,
        MAX(`create_time`) AS `last_time`
    FROM `ticket`
    WHERE `deleted` = 0
      AND `workflow_id` IS NOT NULL
    GROUP BY `workflow_id`
) t ON w.`id` = t.`workflow_id`
SET w.`invocation_count` = IFNULL(t.`cnt`, 0),
    w.`first_invoked_time` = t.`first_time`,
    w.`last_invoked_time` = t.`last_time`
WHERE w.`deleted` = 0;
