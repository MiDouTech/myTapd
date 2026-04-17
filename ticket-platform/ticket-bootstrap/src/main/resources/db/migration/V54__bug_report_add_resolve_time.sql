-- Bug简报：处理完成场景下的「解决时间」（含时分秒），与临时解决场景的日期/方案字段区分存储
SET @col = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bug_report'
      AND COLUMN_NAME = 'resolve_time'
);
SET @sql = IF(
    @col = 0,
    'ALTER TABLE `bug_report` ADD COLUMN `resolve_time` DATETIME NULL COMMENT ''解决时间（处理完成时填写）'' AFTER `resolve_date`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
