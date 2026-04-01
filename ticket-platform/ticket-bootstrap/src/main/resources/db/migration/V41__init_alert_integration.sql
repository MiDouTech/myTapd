-- ============================================================
-- V41: 告警监控接入工单系统
-- 新增告警规则映射配置表和告警事件日志表
-- ============================================================

CREATE TABLE IF NOT EXISTS `alert_rule_mapping` (
    `id`                    bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rule_name`             varchar(200) NOT NULL DEFAULT '' COMMENT '夜莺告警规则名称',
    `match_mode`            varchar(20)  NOT NULL DEFAULT 'EXACT' COMMENT '匹配模式：EXACT-精确匹配, PREFIX-前缀匹配',
    `category_id`           bigint(20)   NOT NULL COMMENT '映射的工单分类ID',
    `priority_p1`           varchar(20)  NOT NULL DEFAULT 'urgent' COMMENT 'severity=1时映射的优先级',
    `priority_p2`           varchar(20)  NOT NULL DEFAULT 'high' COMMENT 'severity=2时映射的优先级',
    `priority_p3`           varchar(20)  NOT NULL DEFAULT 'medium' COMMENT 'severity=3时映射的优先级',
    `assignee_id`           bigint(20)            DEFAULT NULL COMMENT '默认处理人用户ID',
    `dedup_window_minutes`  int(11)      NOT NULL DEFAULT 30 COMMENT '去重时间窗口（分钟）',
    `enabled`               tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：0-停用, 1-启用',
    `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`             varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`             varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`               tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_rule_name` (`rule_name`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则映射配置表';

CREATE TABLE IF NOT EXISTS `alert_event_log` (
    `id`                    bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `event_hash`            varchar(100) NOT NULL DEFAULT '' COMMENT '夜莺事件hash',
    `rule_id`               bigint(20)   NOT NULL DEFAULT 0 COMMENT '夜莺规则ID',
    `rule_name`             varchar(200) NOT NULL DEFAULT '' COMMENT '规则名称',
    `severity`              int(11)      NOT NULL DEFAULT 3 COMMENT '告警级别：1-P1, 2-P2, 3-P3',
    `target_ident`          varchar(200) NOT NULL DEFAULT '' COMMENT '监控对象标识',
    `trigger_value`         varchar(100) NOT NULL DEFAULT '' COMMENT '触发值',
    `trigger_time`          datetime              DEFAULT NULL COMMENT '告警触发时间',
    `is_recovered`          tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否恢复事件',
    `ticket_id`             bigint(20)            DEFAULT NULL COMMENT '关联的工单ID',
    `raw_payload`           text                  DEFAULT NULL COMMENT '原始JSON',
    `process_result`        varchar(20)  NOT NULL DEFAULT '' COMMENT '处理结果：CREATED-已创建工单, DEDUP-去重跳过, RECOVERED-恢复事件, UNMAPPED-无映射, ERROR-处理异常',
    `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`             varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`             varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`               tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_event_hash` (`event_hash`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_trigger_time` (`trigger_time`),
    KEY `idx_process_result` (`process_result`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警事件日志表';
