-- 为 webhook_config 表增加 name 字段，用于区分推送目标
-- 若列已存在（手工加列或其它脚本已加），跳过 ALTER，避免 1060 Duplicate column 导致 Flyway 失败

SET @preparedStatement = (
    SELECT IF(
        (
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'webhook_config'
              AND COLUMN_NAME = 'name'
        ) > 0,
        'SELECT 1',
        'ALTER TABLE `webhook_config` ADD COLUMN `name` varchar(100) NOT NULL DEFAULT '''' COMMENT ''配置名称'' AFTER `id`'
    )
);
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 将已有记录的 name 设为基于 id 的默认值，避免空白
UPDATE `webhook_config` SET `name` = CONCAT('Webhook-', `id`) WHERE `name` = '';
