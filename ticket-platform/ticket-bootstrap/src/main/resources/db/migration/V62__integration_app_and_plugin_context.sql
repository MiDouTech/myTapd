-- ============================================================
-- V62__integration_app_and_plugin_context.sql
-- 业务原生工单插件：接入应用表 + 工单插件上下文字段
-- ============================================================

CREATE TABLE IF NOT EXISTS `integration_app` (
    `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_name`             varchar(100) NOT NULL COMMENT '应用名称',
    `app_key`              varchar(64)  NOT NULL COMMENT 'AppKey',
    `app_secret`           varchar(128) NOT NULL COMMENT 'AppSecret',
    `system_code`          varchar(50)  NOT NULL COMMENT '系统标识，如 xingqiu',
    `default_category_id`  bigint       NOT NULL COMMENT '默认工单分类ID',
    `category_mapping`     json                  DEFAULT NULL COMMENT 'bizType→categoryId 映射',
    `callback_url`         varchar(500)          DEFAULT NULL COMMENT 'Webhook回调地址',
    `callback_secret`      varchar(128)          DEFAULT NULL COMMENT '回调签名密钥',
    `allowed_origins`      varchar(1000)         DEFAULT NULL COMMENT 'CORS白名单，逗号分隔',
    `permissions`          varchar(500) NOT NULL DEFAULT 'plugin:ticket:create,plugin:ticket:read-mine' COMMENT '权限列表',
    `status`               tinyint      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`            varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`            varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`              tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_key` (`app_key`),
    UNIQUE KEY `uk_system_code` (`system_code`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件接入应用';

ALTER TABLE `ticket`
    ADD COLUMN `integration_app_id` bigint DEFAULT NULL COMMENT '接入应用ID' AFTER `source`,
    ADD COLUMN `external_user_id` varchar(100) DEFAULT NULL COMMENT '外部系统用户ID' AFTER `integration_app_id`,
    ADD COLUMN `external_ticket_ref` varchar(100) DEFAULT NULL COMMENT '外部关联单号（幂等）' AFTER `external_user_id`,
    ADD COLUMN `plugin_context` json DEFAULT NULL COMMENT '插件上下文JSON' AFTER `external_ticket_ref`,
    ADD KEY `idx_integration_app` (`integration_app_id`),
    ADD KEY `idx_external_ref` (`integration_app_id`, `external_ticket_ref`);
