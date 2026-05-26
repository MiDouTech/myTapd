-- ============================================================
-- V58__sla_notification_quiet_hours.sql
-- SLA 通知夜间静默：22:00–次日 09:00 延迟发送队列
-- ============================================================

CREATE TABLE IF NOT EXISTS `sla_notification_pending` (
    `id`                  bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`           bigint(20)   NOT NULL COMMENT '工单ID',
    `notification_type`   varchar(50)  NOT NULL COMMENT '通知类型（SLA_WARNING/SLA_BREACHED）',
    `title`               varchar(200) NOT NULL COMMENT '通知标题',
    `content`             text         NOT NULL COMMENT '通知内容',
    `detail_link`         varchar(500)          DEFAULT NULL COMMENT '详情链接',
    `receiver_user_ids`   varchar(500)          DEFAULT NULL COMMENT '站内/企微应用消息接收人ID，逗号分隔',
    `mention_user_ids`    varchar(500)          DEFAULT NULL COMMENT '企微群@用户ID，逗号分隔',
    `scheduled_send_at`   datetime     NOT NULL COMMENT '计划发送时间',
    `sent_at`             datetime              DEFAULT NULL COMMENT '实际发送时间',
    `status`              varchar(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING/SENT/FAILED）',
    `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`           varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`           varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`             tinyint(4)   NOT NULL DEFAULT '0' COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_scheduled_status` (`status`, `scheduled_send_at`),
    KEY `idx_ticket_type_status` (`ticket_id`, `notification_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA通知延迟发送队列';

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'sla_notify_quiet_start', '22:00', 'SLA', 'SLA通知静默开始（HH:mm，含）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'sla_notify_quiet_start' AND `deleted` = 0);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'sla_notify_quiet_end', '09:00', 'SLA', 'SLA通知静默结束（HH:mm，不含，到点可发）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'sla_notify_quiet_end' AND `deleted` = 0);
