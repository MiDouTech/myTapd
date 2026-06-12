-- V57：兜底补齐 ticket_bug_info.manual_valid_report 字段，兼容历史环境
SET @col = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ticket_bug_info'
      AND COLUMN_NAME = 'manual_valid_report'
);
SET @sql = IF(
    @col = 0,
    'ALTER TABLE `ticket_bug_info` ADD COLUMN `manual_valid_report` varchar(8) DEFAULT NULL COMMENT ''手工有效报告（YES:是 NO:否）'' AFTER `problem_screenshot`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
