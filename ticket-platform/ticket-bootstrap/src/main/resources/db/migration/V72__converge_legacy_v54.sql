-- Converge both historical V54 variants without modifying flyway_schema_history.
SET @backfill_workflow = (SELECT COUNT(*) < 3 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='workflow' AND COLUMN_NAME IN ('invocation_count','first_invoked_time','last_invoked_time'));
SET @exists_col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bug_report' AND COLUMN_NAME='resolve_time');
SET @ddl = IF(@exists_col=0, 'ALTER TABLE bug_report ADD COLUMN resolve_time DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists_col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='workflow' AND COLUMN_NAME='invocation_count');
SET @ddl = IF(@exists_col=0, 'ALTER TABLE workflow ADD COLUMN invocation_count BIGINT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists_col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='workflow' AND COLUMN_NAME='first_invoked_time');
SET @ddl = IF(@exists_col=0, 'ALTER TABLE workflow ADD COLUMN first_invoked_time DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists_col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='workflow' AND COLUMN_NAME='last_invoked_time');
SET @ddl = IF(@exists_col=0, 'ALTER TABLE workflow ADD COLUMN last_invoked_time DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='workflow' AND INDEX_NAME='idx_invocation_count');
SET @ddl = IF(@exists_idx=0, 'CREATE INDEX idx_invocation_count ON workflow(invocation_count)', 'SELECT 1');
PREPARE stmt FROM @ddl;
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
WHERE w.`deleted` = 0 AND @backfill_workflow;
