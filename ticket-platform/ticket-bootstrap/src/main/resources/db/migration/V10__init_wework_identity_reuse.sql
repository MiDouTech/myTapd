-- ============================================================
-- V10__init_wework_identity_reuse.sql
-- 企业微信账号体系复用一期：配置、同步日志、派单规则与工单关联增强
-- ============================================================

-- -----------------------------------------------------------
-- 部门表增强：补齐状态与同步信息，增加企微部门ID唯一约束
-- -----------------------------------------------------------
SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'department' AND COLUMN_NAME = 'dept_status'
    ),
    'SELECT 1',
    'ALTER TABLE `department` ADD COLUMN `dept_status` tinyint(4) NOT NULL DEFAULT 1 COMMENT ''部门状态（1:启用 0:停用）'' AFTER `sort_order`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'department' AND COLUMN_NAME = 'sync_status'
    ),
    'SELECT 1',
    'ALTER TABLE `department` ADD COLUMN `sync_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT ''同步状态（0:未同步 1:成功 2:失败）'' AFTER `dept_status`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'department' AND COLUMN_NAME = 'sync_time'
    ),
    'SELECT 1',
    'ALTER TABLE `department` ADD COLUMN `sync_time` datetime DEFAULT NULL COMMENT ''最近同步时间'' AFTER `sync_status`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'department' AND COLUMN_NAME = 'leader_wecom_userid'
    ),
    'SELECT 1',
    'ALTER TABLE `department` ADD COLUMN `leader_wecom_userid` varchar(100) DEFAULT NULL COMMENT ''部门负责人企微UserID'' AFTER `sync_time`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'department' AND INDEX_NAME = 'uk_department_wecom_dept_id'
    ),
    'SELECT 1',
    'ALTER TABLE `department` ADD UNIQUE KEY `uk_department_wecom_dept_id` (`wecom_dept_id`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------
-- 用户表增强：补齐同步信息
-- -----------------------------------------------------------
SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'sync_status'
    ),
    'SELECT 1',
    'ALTER TABLE `sys_user` ADD COLUMN `sync_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT ''同步状态（0:未同步 1:成功 2:失败）'' AFTER `account_status`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'sync_time'
    ),
    'SELECT 1',
    'ALTER TABLE `sys_user` ADD COLUMN `sync_time` datetime DEFAULT NULL COMMENT ''最近同步时间'' AFTER `sync_status`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------
-- 工单表增强：补齐企微账号与当前部门关联字段
-- -----------------------------------------------------------
SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'creator_wework_userid'
    ),
    'SELECT 1',
    'ALTER TABLE `ticket` ADD COLUMN `creator_wework_userid` varchar(100) DEFAULT NULL COMMENT ''创建人企微UserID'' AFTER `creator_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'assignee_wework_userid'
    ),
    'SELECT 1',
    'ALTER TABLE `ticket` ADD COLUMN `assignee_wework_userid` varchar(100) DEFAULT NULL COMMENT ''处理人企微UserID'' AFTER `assignee_id`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'current_dept_id'
    ),
    'SELECT 1',
    'ALTER TABLE `ticket` ADD COLUMN `current_dept_id` bigint(20) DEFAULT NULL COMMENT ''当前处理部门ID'' AFTER `assignee_wework_userid`'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND INDEX_NAME = 'idx_ticket_creator_wework_userid'
    ),
    'SELECT 1',
    'ALTER TABLE `ticket` ADD KEY `idx_ticket_creator_wework_userid` (`creator_wework_userid`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND INDEX_NAME = 'idx_ticket_assignee_wework_userid'
    ),
    'SELECT 1',
    'ALTER TABLE `ticket` ADD KEY `idx_ticket_assignee_wework_userid` (`assignee_wework_userid`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND INDEX_NAME = 'idx_ticket_current_dept_id'
    ),
    'SELECT 1',
    'ALTER TABLE `ticket` ADD KEY `idx_ticket_current_dept_id` (`current_dept_id`)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------
-- 企业微信配置表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_wework_config` (
    `id`                 bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `corp_id`            varchar(100) NOT NULL COMMENT '企业微信CorpID',
    `agent_id`           varchar(50)  NOT NULL COMMENT '企业微信AgentID',
    `corp_secret`        varchar(255) NOT NULL COMMENT '企业微信应用Secret（密文）',
    `api_base_url`       varchar(255) NOT NULL DEFAULT 'https://qyapi.weixin.qq.com' COMMENT '企微API基础地址',
    `connect_timeout_ms` int(11)      NOT NULL DEFAULT 10000 COMMENT '连接超时（毫秒）',
    `read_timeout_ms`    int(11)      NOT NULL DEFAULT 30000 COMMENT '读取超时（毫秒）',
    `schedule_enabled`   tinyint(4)   NOT NULL DEFAULT 0 COMMENT '是否开启定时同步（0:否 1:是）',
    `schedule_cron`      varchar(64)  DEFAULT NULL COMMENT '定时同步Cron表达式',
    `retry_count`        int(11)      NOT NULL DEFAULT 3 COMMENT '失败重试次数',
    `batch_size`         int(11)      NOT NULL DEFAULT 100 COMMENT '同步批次大小',
    `status`             tinyint(4)   NOT NULL DEFAULT 1 COMMENT '配置状态（1:启用 0:停用）',
    `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`          varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`          varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`            tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_corp_agent` (`corp_id`, `agent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='企业微信配置表';

-- -----------------------------------------------------------
-- 同步日志表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_sync_log` (
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sync_type`      varchar(30)  NOT NULL COMMENT '同步类型（DEPARTMENT/EMPLOYEE/FULL）',
    `sync_mode`      varchar(20)  NOT NULL COMMENT '同步模式（MANUAL/SCHEDULE）',
    `sync_status`    varchar(20)  NOT NULL COMMENT '同步状态（SUCCESS/FAILED/PARTIAL）',
    `total_count`    int(11)      NOT NULL DEFAULT 0 COMMENT '总处理数',
    `success_count`  int(11)      NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count`     int(11)      NOT NULL DEFAULT 0 COMMENT '失败数',
    `retry_count`    int(11)      NOT NULL DEFAULT 0 COMMENT '重试次数',
    `duration_ms`    bigint(20)   DEFAULT NULL COMMENT '耗时（毫秒）',
    `trigger_by`     varchar(50)  DEFAULT NULL COMMENT '触发人（定时任务可为空）',
    `error_code`     varchar(100) DEFAULT NULL COMMENT '错误码',
    `error_message`  varchar(1000) DEFAULT NULL COMMENT '错误原因',
    `start_time`     datetime     DEFAULT NULL COMMENT '开始时间',
    `end_time`       datetime     DEFAULT NULL COMMENT '结束时间',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`        tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_sync_type` (`sync_type`),
    KEY `idx_sync_mode` (`sync_mode`),
    KEY `idx_sync_status` (`sync_status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='企微同步日志表';

-- -----------------------------------------------------------
-- 工单用户角色映射（企微账号+本地RBAC）
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ticket_user_role` (
    `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `wework_userid` varchar(100) NOT NULL COMMENT '企微UserID',
    `role_code`     varchar(50) NOT NULL COMMENT '角色编码',
    `status`        tinyint(4)  NOT NULL DEFAULT 1 COMMENT '状态（1:启用 0:停用）',
    `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`       tinyint(4)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_wework_role` (`wework_userid`, `role_code`),
    KEY `idx_wework_userid` (`wework_userid`),
    KEY `idx_role_code` (`role_code`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单用户角色映射表';

-- -----------------------------------------------------------
-- 自动派单规则
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ticket_assignment_rule` (
    `id`                             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_category_id`             bigint(20)   DEFAULT NULL COMMENT '工单分类ID',
    `priority`                       varchar(20)  DEFAULT NULL COMMENT '优先级（URGENT/HIGH/MEDIUM/LOW）',
    `target_dept_id`                 bigint(20)   DEFAULT NULL COMMENT '目标处理部门ID',
    `target_role_code`               varchar(50)  DEFAULT NULL COMMENT '目标角色编码',
    `default_assignee_wework_userid` varchar(100) DEFAULT NULL COMMENT '默认处理人企微UserID',
    `fallback_type`                  varchar(20)  NOT NULL DEFAULT 'ADMIN' COMMENT '兜底类型（ADMIN/QUEUE/NONE）',
    `status`                         tinyint(4)   NOT NULL DEFAULT 1 COMMENT '状态（1:启用 0:停用）',
    `sort_order`                     int(11)      NOT NULL DEFAULT 0 COMMENT '优先级排序（越小越优先）',
    `remark`                         varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time`                    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`                      varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`                      varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`                        tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_category_priority` (`ticket_category_id`, `priority`),
    KEY `idx_target_dept` (`target_dept_id`),
    KEY `idx_target_role` (`target_role_code`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单自动派单规则表';
