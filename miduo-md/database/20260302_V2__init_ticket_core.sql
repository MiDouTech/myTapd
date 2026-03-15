-- ============================================================
-- V2__init_ticket_core.sql
-- 工单核心表：工单主表、分类、模板、自定义字段、评论、日志、附件、关注人
-- ============================================================

-- -----------------------------------------------------------
-- 工单分类表（支持三级分类）
-- -----------------------------------------------------------
CREATE TABLE `ticket_category` (
    `id`              bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`            varchar(100) NOT NULL COMMENT '分类名称',
    `parent_id`       bigint(20)   DEFAULT NULL COMMENT '父分类ID',
    `level`           tinyint(4)   NOT NULL DEFAULT 1 COMMENT '分类层级（1:一级 2:二级 3:三级）',
    `path`            varchar(500) DEFAULT NULL COMMENT '分类全路径（如 /1/2/3/）',
    `template_id`     bigint(20)   DEFAULT NULL COMMENT '关联的工单模板ID',
    `workflow_id`     bigint(20)   DEFAULT NULL COMMENT '关联的工作流ID',
    `sla_policy_id`   bigint(20)   DEFAULT NULL COMMENT '关联的SLA策略ID',
    `default_group_id` bigint(20)  DEFAULT NULL COMMENT '默认处理组ID',
    `sort_order`      int(11)      NOT NULL DEFAULT 0 COMMENT '排序号',
    `is_active`       tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`       varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`         tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_level` (`level`),
    KEY `idx_workflow_id` (`workflow_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单分类表';

-- -----------------------------------------------------------
-- 工单模板表
-- -----------------------------------------------------------
CREATE TABLE `ticket_template` (
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`          varchar(100) NOT NULL COMMENT '模板名称',
    `category_id`   bigint(20)   DEFAULT NULL COMMENT '关联分类ID',
    `fields_config` json         DEFAULT NULL COMMENT '自定义字段配置（JSON格式）',
    `description`   varchar(500) DEFAULT NULL COMMENT '模板描述',
    `is_active`     tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`     varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`       tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单模板表';

-- -----------------------------------------------------------
-- 工单主表
-- -----------------------------------------------------------
CREATE TABLE `ticket` (
    `id`              bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_no`       varchar(50)   NOT NULL COMMENT '工单编号（业务可读，如WO-20260228-001）',
    `title`           varchar(200)  NOT NULL COMMENT '工单标题',
    `description`     text          DEFAULT NULL COMMENT '工单描述（富文本）',
    `category_id`     bigint(20)    DEFAULT NULL COMMENT '工单分类ID',
    `template_id`     bigint(20)    DEFAULT NULL COMMENT '工单模板ID',
    `workflow_id`     bigint(20)    DEFAULT NULL COMMENT '关联工作流ID',
    `priority`        varchar(20)   NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级（URGENT:紧急 HIGH:高 MEDIUM:中 LOW:低）',
    `status`          varchar(50)   NOT NULL DEFAULT 'PENDING' COMMENT '工单状态',
    `creator_id`      bigint(20)    NOT NULL COMMENT '创建人ID',
    `assignee_id`     bigint(20)    DEFAULT NULL COMMENT '当前处理人ID',
    `source`          varchar(30)   NOT NULL DEFAULT 'WEB' COMMENT '来源（WEB:网页 WECOM_BOT:企微群机器人 API:接口）',
    `source_chat_id`  varchar(100)  DEFAULT NULL COMMENT '来源企微群ID（企微群创建时记录）',
    `custom_fields`   json          DEFAULT NULL COMMENT '自定义字段值（JSON格式）',
    `expected_time`   datetime      DEFAULT NULL COMMENT '期望完成时间',
    `resolved_at`     datetime      DEFAULT NULL COMMENT '解决时间',
    `closed_at`       datetime      DEFAULT NULL COMMENT '关闭时间',
    `version`         int(11)       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(50)   NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`       varchar(50)   NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`         tinyint(4)    NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_no` (`ticket_no`),
    KEY `idx_ticket_assignee_status` (`assignee_id`, `status`, `priority`),
    KEY `idx_ticket_creator_status` (`creator_id`, `status`),
    KEY `idx_ticket_category_status` (`category_id`, `status`),
    KEY `idx_ticket_created_at` (`create_time` DESC),
    KEY `idx_ticket_updated_at` (`update_time` DESC),
    KEY `idx_ticket_sla` (`status`, `priority`, `create_time`),
    KEY `idx_ticket_source` (`source`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单主表';

-- -----------------------------------------------------------
-- 工单自定义字段值表（EAV搜索索引）
-- -----------------------------------------------------------
CREATE TABLE `ticket_custom_field` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`   bigint(20)   NOT NULL COMMENT '工单ID',
    `field_key`   varchar(100) NOT NULL COMMENT '字段键名',
    `field_value` text         DEFAULT NULL COMMENT '字段值',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_field` (`ticket_id`, `field_key`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单自定义字段值表';

-- -----------------------------------------------------------
-- 工单评论/处理记录表
-- -----------------------------------------------------------
CREATE TABLE `ticket_comment` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`   bigint(20)   NOT NULL COMMENT '工单ID',
    `user_id`     bigint(20)   NOT NULL COMMENT '评论用户ID',
    `content`     text         NOT NULL COMMENT '评论内容',
    `type`        varchar(20)  NOT NULL DEFAULT 'COMMENT' COMMENT '类型（COMMENT:评论 OPERATION:操作记录）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单评论/处理记录表';

-- -----------------------------------------------------------
-- 工单操作日志表
-- -----------------------------------------------------------
CREATE TABLE `ticket_log` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`   bigint(20)   NOT NULL COMMENT '工单ID',
    `user_id`     bigint(20)   NOT NULL COMMENT '操作用户ID',
    `action`      varchar(50)  NOT NULL COMMENT '操作类型（CREATE/UPDATE/ASSIGN/TRANSFER/CLOSE/REOPEN等）',
    `old_value`   text         DEFAULT NULL COMMENT '变更前的值',
    `new_value`   text         DEFAULT NULL COMMENT '变更后的值',
    `remark`      varchar(500) DEFAULT NULL COMMENT '操作备注',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单操作日志表';

-- -----------------------------------------------------------
-- 工单附件表
-- -----------------------------------------------------------
CREATE TABLE `ticket_attachment` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`   bigint(20)   NOT NULL COMMENT '工单ID',
    `file_name`   varchar(255) NOT NULL COMMENT '文件名',
    `file_path`   varchar(500) NOT NULL COMMENT '文件存储路径',
    `file_size`   bigint(20)   NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `file_type`   varchar(100) DEFAULT NULL COMMENT '文件MIME类型',
    `uploaded_by`  bigint(20)  NOT NULL COMMENT '上传人ID',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单附件表';

-- -----------------------------------------------------------
-- 工单关注人表
-- -----------------------------------------------------------
CREATE TABLE `ticket_follower` (
    `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`   bigint(20)  NOT NULL COMMENT '工单ID',
    `user_id`     bigint(20)  NOT NULL COMMENT '关注人ID',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_user` (`ticket_id`, `user_id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单关注人表';
