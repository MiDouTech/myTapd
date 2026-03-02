-- ============================================================
-- V8__init_task010_webhook.sql
-- Task010：Webhook配置表
-- ============================================================

CREATE TABLE `webhook_config` (
    `id`                 bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `url`                varchar(500)  NOT NULL COMMENT 'Webhook回调地址',
    `secret`             varchar(200)  DEFAULT NULL COMMENT '签名密钥',
    `event_types`        varchar(500)  NOT NULL COMMENT '订阅事件类型（逗号分隔）',
    `is_active`          tinyint(4)    NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `timeout_ms`         int(11)       NOT NULL DEFAULT 5000 COMMENT '超时时间（毫秒）',
    `max_retry_times`    int(11)       NOT NULL DEFAULT 0 COMMENT '失败重试次数',
    `description`        varchar(500)  DEFAULT NULL COMMENT '配置说明',
    `last_success_time`  datetime      DEFAULT NULL COMMENT '最近成功推送时间',
    `last_fail_time`     datetime      DEFAULT NULL COMMENT '最近失败推送时间',
    `last_fail_reason`   varchar(1000) DEFAULT NULL COMMENT '最近失败原因',
    `create_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`          varchar(50)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`          varchar(50)   NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`            tinyint(4)    NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_webhook_active` (`is_active`),
    KEY `idx_webhook_create_time` (`create_time`),
    KEY `idx_webhook_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Webhook配置表';
