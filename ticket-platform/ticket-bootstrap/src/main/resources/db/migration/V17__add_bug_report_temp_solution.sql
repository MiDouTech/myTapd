-- Bug简报新增临时解决方案和临时解决日期字段
-- 对应小工具 easy-bug 中的 临时解决方案(tempsolution) 和 临时解决时间(tempsolvetime)
-- 幂等：列已存在则跳过

SET @col1 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bug_report' AND COLUMN_NAME = 'temp_resolve_date');
SET @sql1 = IF(@col1 = 0, 'ALTER TABLE `bug_report` ADD COLUMN `temp_resolve_date` DATE NULL COMMENT ''临时解决日期'' AFTER `resolve_date`', 'SELECT 1');
PREPARE stmt1 FROM @sql1; EXECUTE stmt1; DEALLOCATE PREPARE stmt1;

SET @col2 = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bug_report' AND COLUMN_NAME = 'temp_solution');
SET @sql2 = IF(@col2 = 0, 'ALTER TABLE `bug_report` ADD COLUMN `temp_solution` TEXT NULL COMMENT ''临时解决方案'' AFTER `solution`', 'SELECT 1');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
