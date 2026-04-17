-- 修复SLA计时器恢复后累计时间丢失问题：新增 base_elapsed_minutes 字段
-- 恢复时保存暂停前的已累计工作时间，checkSingleTimer 计算时 = base_elapsed_minutes + 恢复后经过的工作时间
-- 若列已存在（曾手动加列或迁移部分执行），跳过 ALTER，避免 Flyway 1060 Duplicate column

SET @preparedStatement = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'sla_timer'
              AND COLUMN_NAME = 'base_elapsed_minutes'
        ) > 0,
        'SELECT 1',
        'ALTER TABLE `sla_timer` ADD COLUMN `base_elapsed_minutes` int(11) NOT NULL DEFAULT 0 COMMENT ''恢复前累计工作时间（分钟），用于挂起后恢复时正确累加'' AFTER `elapsed_minutes`'
    )
);
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
