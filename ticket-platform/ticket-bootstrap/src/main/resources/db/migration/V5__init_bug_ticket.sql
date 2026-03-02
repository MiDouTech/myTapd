-- ============================================================
-- V5__init_bug_ticket.sql
-- 缺陷工单扩展表：客服信息、测试信息、开发信息
-- ============================================================

-- -----------------------------------------------------------
-- 缺陷工单客服信息表（客服填写）
-- -----------------------------------------------------------
CREATE TABLE `ticket_bug_info` (
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`        bigint(20)   NOT NULL COMMENT '工单ID',
    `merchant_no`      varchar(50)  DEFAULT NULL COMMENT '商户编号',
    `company_name`     varchar(200) DEFAULT NULL COMMENT '公司名称',
    `merchant_account` varchar(50)  DEFAULT NULL COMMENT '商户账号',
    `problem_desc`     text         DEFAULT NULL COMMENT '问题描述',
    `expected_result`  text         DEFAULT NULL COMMENT '预期结果',
    `scene_code`       varchar(100) DEFAULT NULL COMMENT '场景码',
    `create_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`        varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`          tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_id` (`ticket_id`),
    KEY `idx_merchant_no` (`merchant_no`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='缺陷工单客服信息表';

-- -----------------------------------------------------------
-- 缺陷工单测试信息表（测试填写）
-- -----------------------------------------------------------
CREATE TABLE `ticket_bug_test_info` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`       bigint(20)   NOT NULL COMMENT '工单ID',
    `reproduce_env`   varchar(50)  DEFAULT NULL COMMENT '复现环境（PRODUCTION:生产环境 TEST:测试环境 BOTH:均可复现）',
    `reproduce_steps` text         DEFAULT NULL COMMENT '复现步骤',
    `actual_result`   text         DEFAULT NULL COMMENT '实际结果',
    `impact_scope`    varchar(50)  DEFAULT NULL COMMENT '影响范围（SINGLE:单一商户 PARTIAL:部分商户 ALL:全部商户）',
    `severity_level`  varchar(20)  DEFAULT NULL COMMENT '缺陷等级（FATAL:致命 CRITICAL:严重 NORMAL:一般 MINOR:轻微）',
    `module_name`     varchar(100) DEFAULT NULL COMMENT '所属模块',
    `test_remark`     text         DEFAULT NULL COMMENT '测试备注',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`         tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_id` (`ticket_id`),
    KEY `idx_severity_level` (`severity_level`),
    KEY `idx_module_name` (`module_name`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='缺陷工单测试信息表';

-- -----------------------------------------------------------
-- 缺陷工单开发信息表（开发填写）
-- -----------------------------------------------------------
CREATE TABLE `ticket_bug_dev_info` (
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`         bigint(20)   NOT NULL COMMENT '工单ID',
    `root_cause`        text         DEFAULT NULL COMMENT '缺陷原因',
    `fix_solution`      text         DEFAULT NULL COMMENT '修复方案',
    `git_branch`        varchar(200) DEFAULT NULL COMMENT '关联分支/提交',
    `impact_assessment` text         DEFAULT NULL COMMENT '影响范围评估',
    `dev_remark`        text         DEFAULT NULL COMMENT '开发备注',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`         varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`         varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`           tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_id` (`ticket_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='缺陷工单开发信息表';
