-- ============================================================
-- V7__init_wecom_notification.sql
-- 企微集成表、通知记录表、通知偏好表、系统配置表
-- ============================================================

-- -----------------------------------------------------------
-- 企微群绑定配置表
-- -----------------------------------------------------------
CREATE TABLE `wecom_group_binding` (
    `id`                  bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `chat_id`             varchar(100) NOT NULL COMMENT '企微群ChatID',
    `chat_name`           varchar(200) DEFAULT NULL COMMENT '群名称',
    `default_category_id` bigint(20)   DEFAULT NULL COMMENT '默认工单分类ID',
    `webhook_url`         varchar(500) DEFAULT NULL COMMENT '群Webhook推送地址',
    `is_active`           tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`           varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`           varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`             tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_id` (`chat_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='企微群绑定配置表';

-- -----------------------------------------------------------
-- 企微机器人消息日志表
-- -----------------------------------------------------------
CREATE TABLE `wecom_bot_message_log` (
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `chat_id`           varchar(100) NOT NULL COMMENT '来源群ChatID',
    `msg_id`            varchar(100) DEFAULT NULL COMMENT '企微消息ID（用于去重）',
    `from_wecom_userid` varchar(100) NOT NULL COMMENT '发送人企微UserID',
    `raw_message`       text         NOT NULL COMMENT '原始消息内容',
    `parsed_result`     json         DEFAULT NULL COMMENT '解析结果（JSON格式）',
    `ticket_id`         bigint(20)   DEFAULT NULL COMMENT '创建的工单ID',
    `status`            varchar(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '处理状态（SUCCESS:成功 FAIL:失败 DUPLICATE:重复）',
    `error_msg`         varchar(500) DEFAULT NULL COMMENT '错误信息',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`         varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`           tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_chat_id` (`chat_id`),
    KEY `idx_msg_id` (`msg_id`),
    KEY `idx_from_wecom_userid` (`from_wecom_userid`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='企微机器人消息日志表';

-- -----------------------------------------------------------
-- 通知记录表
-- -----------------------------------------------------------
CREATE TABLE `notification` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     bigint(20)   NOT NULL COMMENT '目标用户ID',
    `ticket_id`   bigint(20)   DEFAULT NULL COMMENT '关联工单ID',
    `report_id`   bigint(20)   DEFAULT NULL COMMENT '关联简报ID',
    `type`        varchar(50)  NOT NULL COMMENT '通知类型（TICKET_CREATED/STATUS_CHANGED/ASSIGNED/SLA_WARNING/SLA_BREACHED/COMMENT/URGE/REPORT_REMIND等）',
    `channel`     varchar(20)  NOT NULL DEFAULT 'SITE' COMMENT '渠道（SITE:站内信 WECOM_APP:企微应用消息 WECOM_GROUP:企微群 EMAIL:邮件）',
    `title`       varchar(200) NOT NULL COMMENT '通知标题',
    `content`     text         NOT NULL COMMENT '通知内容',
    `is_read`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '是否已读（0:未读 1:已读）',
    `read_at`     datetime     DEFAULT NULL COMMENT '阅读时间',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_type` (`type`),
    KEY `idx_channel` (`channel`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';

-- -----------------------------------------------------------
-- 用户通知偏好表
-- -----------------------------------------------------------
CREATE TABLE `notification_preference` (
    `id`           bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      bigint(20)  NOT NULL COMMENT '用户ID',
    `event_type`   varchar(50) NOT NULL COMMENT '事件类型（与notification.type对应）',
    `site_enabled` tinyint(4)  NOT NULL DEFAULT 1 COMMENT '站内信开关（0:关闭 1:开启）',
    `wecom_enabled` tinyint(4) NOT NULL DEFAULT 1 COMMENT '企微消息开关（0:关闭 1:开启）',
    `email_enabled` tinyint(4) NOT NULL DEFAULT 0 COMMENT '邮件开关（0:关闭 1:开启）',
    `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`    varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`      tinyint(4)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_event` (`user_id`, `event_type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户通知偏好表';

-- -----------------------------------------------------------
-- 系统配置表（键值对存储）
-- -----------------------------------------------------------
CREATE TABLE `system_config` (
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key`    varchar(100) NOT NULL COMMENT '配置键',
    `config_value`  text         NOT NULL COMMENT '配置值',
    `config_group`  varchar(50)  NOT NULL DEFAULT 'DEFAULT' COMMENT '配置分组（DEFAULT/WORKING_TIME/HOLIDAY/TICKET/WECOM/SLA）',
    `description`   varchar(500) DEFAULT NULL COMMENT '配置说明',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`       tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_group` (`config_group`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- -----------------------------------------------------------
-- 初始化系统配置
-- -----------------------------------------------------------
INSERT INTO `system_config` (`id`, `config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`) VALUES
(1, 'working_time_start', '09:00', 'WORKING_TIME', '工作时间开始（HH:mm）', 'system', 'system'),
(2, 'working_time_end', '18:00', 'WORKING_TIME', '工作时间结束（HH:mm）', 'system', 'system'),
(3, 'working_days', '1,2,3,4,5', 'WORKING_TIME', '工作日（1=周一 ... 7=周日，逗号分隔）', 'system', 'system'),
(4, 'ticket_no_prefix', 'WO', 'TICKET', '通用工单编号前缀', 'system', 'system'),
(5, 'bug_ticket_no_prefix', 'BUG', 'TICKET', '缺陷工单编号前缀', 'system', 'system'),
(6, 'bug_report_no_prefix', 'BR', 'TICKET', 'Bug简报编号前缀', 'system', 'system'),
(7, 'attachment_max_size_mb', '20', 'TICKET', '附件最大文件大小（MB）', 'system', 'system'),
(8, 'auto_close_days', '7', 'TICKET', '待验收状态超过N天自动关闭', 'system', 'system'),
(9, 'bug_report_remind_days', '3', 'TICKET', '工单关闭后N天未填写简报自动催促', 'system', 'system'),
(10, 'notification_aggregate_minutes', '5', 'DEFAULT', '同一工单N分钟内的多次变更合并为一条通知', 'system', 'system'),
(11, 'wecom_corp_id', '', 'WECOM', '企业微信CorpID', 'system', 'system'),
(12, 'wecom_agent_id', '', 'WECOM', '企微自建应用AgentID', 'system', 'system'),
(13, 'wecom_secret', '', 'WECOM', '企微应用Secret', 'system', 'system'),
(14, 'wecom_callback_token', '', 'WECOM', '企微回调Token', 'system', 'system'),
(15, 'wecom_callback_aes_key', '', 'WECOM', '企微回调EncodingAESKey', 'system', 'system');
