-- ============================================================
-- V11__init_webhook_dispatch_log.sql
-- Webhook推送明细日志表
-- ============================================================

CREATE TABLE IF NOT EXISTS `webhook_dispatch_log` (
    `id`                bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `webhook_config_id` bigint(20)    DEFAULT NULL COMMENT 'Webhook配置ID（未命中配置时为空）',
    `event_type`        varchar(50)   NOT NULL COMMENT '事件类型（TICKET_CREATED/TICKET_STATUS_CHANGED/TICKET_ASSIGNED）',
    `ticket_id`         bigint(20)    DEFAULT NULL COMMENT '工单ID',
    `request_url`       varchar(500)  DEFAULT NULL COMMENT '请求地址（脱敏）',
    `request_body`      text          DEFAULT NULL COMMENT '请求体摘要',
    `attempt_no`        int(11)       NOT NULL DEFAULT 0 COMMENT '当前尝试次数（从1开始，0表示未发起请求）',
    `max_attempts`      int(11)       NOT NULL DEFAULT 0 COMMENT '最大尝试次数（含首次）',
    `status`            varchar(20)   NOT NULL COMMENT '推送状态（SUCCESS/FAIL/SKIPPED）',
    `response_code`     int(11)       DEFAULT NULL COMMENT 'HTTP响应码',
    `response_body`     text          DEFAULT NULL COMMENT '响应体摘要',
    `fail_reason`       varchar(1000) DEFAULT NULL COMMENT '失败原因',
    `duration_ms`       bigint(20)    DEFAULT NULL COMMENT '本次请求耗时（毫秒）',
    `dispatch_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分发时间',
    `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         varchar(50)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`         varchar(50)   NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`           tinyint(4)    NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_webhook_config_id` (`webhook_config_id`),
    KEY `idx_event_ticket` (`event_type`, `ticket_id`),
    KEY `idx_status` (`status`),
    KEY `idx_dispatch_time` (`dispatch_time`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Webhook推送明细日志表';
