-- V57 used MariaDB-only ADD COLUMN IF NOT EXISTS. Preserve it solely for
-- validating databases where it has already succeeded. New MySQL installs use V56;
-- this forward migration also covers databases baselined beyond V56.
SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ticket_bug_info' AND COLUMN_NAME='manual_valid_report');
SET @ddl = IF(@column_exists=0,
    'ALTER TABLE ticket_bug_info ADD COLUMN manual_valid_report varchar(8) DEFAULT NULL COMMENT ''手工有效报告（YES:是 NO:否）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
