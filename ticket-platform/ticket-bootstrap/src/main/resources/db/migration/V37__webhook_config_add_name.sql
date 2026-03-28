-- 为webhook_config表增加name字段，用于区分推送目标
ALTER TABLE `webhook_config`
    ADD COLUMN `name` varchar(100) NOT NULL DEFAULT '' COMMENT '配置名称' AFTER `id`;

-- 将已有记录的name设为基于url的默认值，避免空白
UPDATE `webhook_config` SET `name` = CONCAT('Webhook-', `id`) WHERE `name` = '';
