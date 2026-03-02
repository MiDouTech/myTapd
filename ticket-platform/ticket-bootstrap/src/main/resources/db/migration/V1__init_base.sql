-- ============================================================
-- V1__init_base.sql
-- 用户、部门、组织架构基础表
-- ============================================================

-- -----------------------------------------------------------
-- 部门表
-- -----------------------------------------------------------
CREATE TABLE `department` (
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`           varchar(100) NOT NULL COMMENT '部门名称',
    `parent_id`      bigint(20)   DEFAULT NULL COMMENT '父部门ID',
    `wecom_dept_id`  bigint(20)   DEFAULT NULL COMMENT '企微部门ID',
    `sort_order`     int(11)      NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`        tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_wecom_dept_id` (`wecom_dept_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- -----------------------------------------------------------
-- 用户表
-- -----------------------------------------------------------
CREATE TABLE `sys_user` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`            varchar(50)  NOT NULL COMMENT '姓名',
    `employee_no`     varchar(50)  DEFAULT NULL COMMENT '工号',
    `department_id`   bigint(20)   DEFAULT NULL COMMENT '所属部门ID',
    `email`           varchar(100) DEFAULT NULL COMMENT '邮箱',
    `phone`           varchar(20)  DEFAULT NULL COMMENT '手机号',
    `position`        varchar(100) DEFAULT NULL COMMENT '职位',
    `avatar_url`      varchar(500) DEFAULT NULL COMMENT '头像URL',
    `wecom_userid`    varchar(100) DEFAULT NULL COMMENT '企微用户标识',
    `account_status`  tinyint(4)   NOT NULL DEFAULT 1 COMMENT '账号状态（1:已激活 2:已禁用 4:未激活）',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`         tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wecom_userid` (`wecom_userid`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_employee_no` (`employee_no`),
    KEY `idx_account_status` (`account_status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- -----------------------------------------------------------
-- 角色表
-- -----------------------------------------------------------
CREATE TABLE `sys_role` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code`   varchar(50)  NOT NULL COMMENT '角色编码',
    `role_name`   varchar(100) NOT NULL COMMENT '角色名称',
    `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- -----------------------------------------------------------
-- 用户角色关联表
-- -----------------------------------------------------------
CREATE TABLE `sys_user_role` (
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     bigint(20) NOT NULL COMMENT '用户ID',
    `role_id`     bigint(20) NOT NULL COMMENT '角色ID',
    `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- -----------------------------------------------------------
-- 初始化角色数据
-- -----------------------------------------------------------
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `create_by`, `update_by`) VALUES
(1, 'ADMIN',     '系统管理员', '全部权限：系统配置、分类管理、工作流管理、用户管理', 'system', 'system'),
(2, 'TICKET_ADMIN', '工单管理员', '所有工单的查看、分派、转派、关闭', 'system', 'system'),
(3, 'HANDLER',   '处理人', '处理分配给自己的工单，查看相关工单', 'system', 'system'),
(4, 'SUBMITTER', '提交人', '创建工单、查看自己的工单、验收、催办', 'system', 'system'),
(5, 'OBSERVER',  '观察者', '仅查看权限，查看范围受部门限制', 'system', 'system');
