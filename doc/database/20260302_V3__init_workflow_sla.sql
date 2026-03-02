-- ============================================================
-- V3__init_workflow_sla.sql
-- 工作流、SLA策略、处理组、分派规则表
-- ============================================================

-- -----------------------------------------------------------
-- 工作流定义表
-- -----------------------------------------------------------
CREATE TABLE `workflow` (
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`         varchar(100) NOT NULL COMMENT '工作流名称',
    `mode`         varchar(20)  NOT NULL DEFAULT 'SIMPLE' COMMENT '模式（SIMPLE:简单模式 ADVANCED:高级模式）',
    `description`  varchar(500) DEFAULT NULL COMMENT '工作流描述',
    `states`       json         NOT NULL COMMENT '状态定义（JSON格式）',
    `transitions`  json         NOT NULL COMMENT '流转规则（JSON格式）',
    `is_builtin`   tinyint(4)   NOT NULL DEFAULT 0 COMMENT '是否内置工作流（0:否 1:是）',
    `is_active`    tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`    varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`      tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_mode` (`mode`),
    KEY `idx_is_builtin` (`is_builtin`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义表';

-- -----------------------------------------------------------
-- SLA策略表
-- -----------------------------------------------------------
CREATE TABLE `sla_policy` (
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`           varchar(100) NOT NULL COMMENT '策略名称',
    `priority`       varchar(20)  NOT NULL COMMENT '适用优先级（URGENT/HIGH/MEDIUM/LOW）',
    `response_time`  int(11)      NOT NULL COMMENT '首次响应时限（分钟）',
    `resolve_time`   int(11)      NOT NULL COMMENT '解决时限（分钟）',
    `warning_pct`    int(11)      NOT NULL DEFAULT 75 COMMENT '预警百分比阈值',
    `critical_pct`   int(11)      NOT NULL DEFAULT 90 COMMENT '告警百分比阈值',
    `description`    varchar(500) DEFAULT NULL COMMENT '策略描述',
    `is_active`      tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`        tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_priority` (`priority`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='SLA策略表';

-- -----------------------------------------------------------
-- 处理组表
-- -----------------------------------------------------------
CREATE TABLE `handler_group` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(100) NOT NULL COMMENT '处理组名称',
    `leader_id`   bigint(20)   DEFAULT NULL COMMENT '组长用户ID',
    `description` varchar(500) DEFAULT NULL COMMENT '处理组描述',
    `skill_tags`  varchar(500) DEFAULT NULL COMMENT '技能标签（逗号分隔）',
    `is_active`   tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_leader_id` (`leader_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='处理组表';

-- -----------------------------------------------------------
-- 处理组成员关系表
-- -----------------------------------------------------------
CREATE TABLE `handler_group_member` (
    `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_id`    bigint(20)  NOT NULL COMMENT '处理组ID',
    `user_id`     bigint(20)  NOT NULL COMMENT '成员用户ID',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='处理组成员关系表';

-- -----------------------------------------------------------
-- 分派规则表
-- -----------------------------------------------------------
CREATE TABLE `dispatch_rule` (
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`           varchar(100) NOT NULL COMMENT '规则名称',
    `category_id`    bigint(20)   DEFAULT NULL COMMENT '关联分类ID',
    `strategy`       varchar(30)  NOT NULL DEFAULT 'MANUAL' COMMENT '分派策略（MANUAL:手动 CATEGORY_DEFAULT:分类默认 ROUND_ROBIN:轮询 LOAD_BALANCE:负载均衡 MATRIX:矩阵分派）',
    `target_group_id` bigint(20)  DEFAULT NULL COMMENT '目标处理组ID',
    `target_user_id` bigint(20)   DEFAULT NULL COMMENT '目标处理人ID',
    `rule_config`    json         DEFAULT NULL COMMENT '规则配置（JSON格式，矩阵分派时存储条件匹配规则）',
    `priority_order` int(11)      NOT NULL DEFAULT 0 COMMENT '规则优先级（数字越小优先级越高）',
    `is_active`      tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`        tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_strategy` (`strategy`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='分派规则表';

-- -----------------------------------------------------------
-- SLA计时器表（记录每个工单的SLA实时状态）
-- -----------------------------------------------------------
CREATE TABLE `sla_timer` (
    `id`                bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`         bigint(20)  NOT NULL COMMENT '工单ID',
    `sla_policy_id`     bigint(20)  NOT NULL COMMENT 'SLA策略ID',
    `timer_type`        varchar(20) NOT NULL COMMENT '计时器类型（RESPONSE:响应 RESOLVE:解决）',
    `status`            varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态（RUNNING:运行中 PAUSED:已暂停 COMPLETED:已完成 BREACHED:已超时）',
    `threshold_minutes` int(11)     NOT NULL COMMENT '时限（分钟）',
    `elapsed_minutes`   int(11)     NOT NULL DEFAULT 0 COMMENT '已消耗工作时间（分钟）',
    `start_at`          datetime    NOT NULL COMMENT '计时开始时间',
    `pause_at`          datetime    DEFAULT NULL COMMENT '暂停时间',
    `deadline`          datetime    DEFAULT NULL COMMENT '截止时间（预计算的工作时间截止点）',
    `breached_at`       datetime    DEFAULT NULL COMMENT '超时时间',
    `completed_at`      datetime    DEFAULT NULL COMMENT '完成时间',
    `is_warned`         tinyint(4)  NOT NULL DEFAULT 0 COMMENT '是否已预警（0:否 1:是）',
    `is_breached`       tinyint(4)  NOT NULL DEFAULT 0 COMMENT '是否已超时（0:否 1:是）',
    `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`         varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`           tinyint(4)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_status` (`status`),
    KEY `idx_timer_type_status` (`timer_type`, `status`),
    KEY `idx_deadline` (`deadline`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='SLA计时器表';

-- -----------------------------------------------------------
-- 初始化内置工作流
-- -----------------------------------------------------------
INSERT INTO `workflow` (`id`, `name`, `mode`, `description`, `states`, `transitions`, `is_builtin`, `create_by`, `update_by`) VALUES
(1, '通用工单工作流', 'SIMPLE', '适用于一般工单的基础状态流转',
 '[{"code":"PENDING","name":"待受理","type":"INITIAL","slaAction":"START_RESPONSE"},{"code":"PROCESSING","name":"处理中","type":"INTERMEDIATE","slaAction":"START_RESOLVE"},{"code":"SUSPENDED","name":"已挂起","type":"INTERMEDIATE","slaAction":"PAUSE"},{"code":"PENDING_VERIFY","name":"待验收","type":"INTERMEDIATE","slaAction":"PAUSE"},{"code":"COMPLETED","name":"已完成","type":"TERMINAL","slaAction":"STOP"},{"code":"CLOSED","name":"已关闭","type":"TERMINAL","slaAction":"STOP"}]',
 '[{"from":"PENDING","to":"PROCESSING","name":"受理","allowedRoles":["HANDLER","ADMIN"]},{"from":"PROCESSING","to":"SUSPENDED","name":"挂起","allowedRoles":["HANDLER","ADMIN"]},{"from":"PROCESSING","to":"PENDING_VERIFY","name":"处理完成","allowedRoles":["HANDLER","ADMIN"]},{"from":"PROCESSING","to":"PENDING","name":"转派","allowedRoles":["HANDLER","ADMIN"]},{"from":"SUSPENDED","to":"PROCESSING","name":"恢复处理","allowedRoles":["HANDLER","ADMIN"]},{"from":"PENDING_VERIFY","to":"COMPLETED","name":"验收通过","allowedRoles":["SUBMITTER","ADMIN"]},{"from":"PENDING_VERIFY","to":"PROCESSING","name":"验收不通过","allowedRoles":["SUBMITTER","ADMIN"]},{"from":"COMPLETED","to":"PENDING","name":"重新打开","allowedRoles":["SUBMITTER","ADMIN"]},{"from":"PENDING","to":"CLOSED","name":"关闭","allowedRoles":["SUBMITTER","ADMIN"]}]',
 1, 'system', 'system'),

(2, '审批工单工作流', 'ADVANCED', '适用于需要审批的工单类型',
 '[{"code":"SUBMITTED","name":"已提交","type":"INITIAL","slaAction":"START_RESPONSE"},{"code":"DEPT_APPROVAL","name":"部门审批","type":"INTERMEDIATE","slaAction":"START_RESOLVE"},{"code":"EXECUTING","name":"执行中","type":"INTERMEDIATE","slaAction":"START_RESOLVE"},{"code":"COMPLETED","name":"已完成","type":"TERMINAL","slaAction":"STOP"},{"code":"REJECTED","name":"已驳回","type":"TERMINAL","slaAction":"STOP"}]',
 '[{"from":"SUBMITTED","to":"DEPT_APPROVAL","name":"提交审批","allowedRoles":["SUBMITTER"]},{"from":"DEPT_APPROVAL","to":"EXECUTING","name":"审批通过","allowedRoles":["HANDLER","ADMIN"]},{"from":"DEPT_APPROVAL","to":"REJECTED","name":"驳回","allowedRoles":["HANDLER","ADMIN"]},{"from":"REJECTED","to":"SUBMITTED","name":"修改重提","allowedRoles":["SUBMITTER"]},{"from":"EXECUTING","to":"COMPLETED","name":"完成","allowedRoles":["HANDLER","ADMIN"]}]',
 1, 'system', 'system'),

(3, '缺陷工单工作流', 'ADVANCED', '缺陷工单专属流转，支持客服→测试→开发→验收→客服确认全链路',
 '[{"code":"PENDING_DISPATCH","name":"待分派","type":"INITIAL","slaAction":"START_RESPONSE"},{"code":"PENDING_TEST","name":"待测试受理","type":"INTERMEDIATE","slaAction":"START_RESPONSE"},{"code":"TESTING","name":"测试中","type":"INTERMEDIATE","slaAction":"START_RESOLVE"},{"code":"PENDING_DEV","name":"待开发受理","type":"INTERMEDIATE","slaAction":"START_RESOLVE"},{"code":"DEVELOPING","name":"开发中","type":"INTERMEDIATE","slaAction":"START_RESOLVE"},{"code":"PENDING_VERIFY","name":"待验收","type":"INTERMEDIATE","slaAction":"START_RESOLVE"},{"code":"PENDING_CS_CONFIRM","name":"待客服确认","type":"INTERMEDIATE","slaAction":"START_RESOLVE"},{"code":"COMPLETED","name":"已完成","type":"TERMINAL","slaAction":"STOP"},{"code":"CLOSED","name":"已关闭","type":"TERMINAL","slaAction":"STOP"}]',
 '[{"from":"PENDING_DISPATCH","to":"PENDING_TEST","name":"分派测试","allowedRoles":["ADMIN","TICKET_ADMIN"]},{"from":"PENDING_TEST","to":"TESTING","name":"受理","allowedRoles":["HANDLER"]},{"from":"PENDING_TEST","to":"PENDING_TEST","name":"转派测试","allowedRoles":["HANDLER"]},{"from":"TESTING","to":"PENDING_DEV","name":"确认缺陷转开发","allowedRoles":["HANDLER"]},{"from":"TESTING","to":"PENDING_TEST","name":"转派其他测试","allowedRoles":["HANDLER"]},{"from":"TESTING","to":"CLOSED","name":"非缺陷关闭","allowedRoles":["HANDLER","ADMIN"]},{"from":"PENDING_DEV","to":"DEVELOPING","name":"受理","allowedRoles":["HANDLER"]},{"from":"PENDING_DEV","to":"PENDING_DEV","name":"转派开发","allowedRoles":["HANDLER"]},{"from":"DEVELOPING","to":"PENDING_VERIFY","name":"修复完成","allowedRoles":["HANDLER"]},{"from":"DEVELOPING","to":"PENDING_DEV","name":"转派其他开发","allowedRoles":["HANDLER"]},{"from":"PENDING_VERIFY","to":"PENDING_CS_CONFIRM","name":"验收通过","allowedRoles":["HANDLER"]},{"from":"PENDING_VERIFY","to":"DEVELOPING","name":"验收不通过退回开发","allowedRoles":["HANDLER"]},{"from":"PENDING_CS_CONFIRM","to":"COMPLETED","name":"客服确认关闭","allowedRoles":["HANDLER","SUBMITTER"]},{"from":"PENDING_CS_CONFIRM","to":"TESTING","name":"客户仍有问题退回测试","allowedRoles":["HANDLER","SUBMITTER"]},{"from":"PENDING_DISPATCH","to":"CLOSED","name":"直接关闭","allowedRoles":["ADMIN"]}]',
 1, 'system', 'system');

-- -----------------------------------------------------------
-- 初始化SLA策略
-- -----------------------------------------------------------
INSERT INTO `sla_policy` (`id`, `name`, `priority`, `response_time`, `resolve_time`, `warning_pct`, `critical_pct`, `description`, `create_by`, `update_by`) VALUES
(1, '紧急SLA策略', 'URGENT', 15, 120, 75, 90, '生产系统故障、全公司影响：15分钟响应，2小时解决', 'system', 'system'),
(2, '高SLA策略',   'HIGH',   30, 240, 75, 90, '部门级影响、核心功能故障：30分钟响应，4小时解决', 'system', 'system'),
(3, '中SLA策略',   'MEDIUM', 120, 480, 75, 90, '个人工作受影响：2小时响应，8小时（1工作日）解决', 'system', 'system'),
(4, '低SLA策略',   'LOW',    240, 1440, 75, 90, '咨询类、优化建议：4小时响应，24小时（3工作日）解决', 'system', 'system');
