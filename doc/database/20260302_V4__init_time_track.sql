-- ============================================================
-- V4__init_time_track.sql
-- 工单时间追踪记录表、节点耗时统计汇总表
-- ============================================================

-- -----------------------------------------------------------
-- 工单时间追踪记录表
-- -----------------------------------------------------------
CREATE TABLE `ticket_time_track` (
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`      bigint(20)   NOT NULL COMMENT '工单ID',
    `user_id`        bigint(20)   DEFAULT NULL COMMENT '操作用户ID',
    `user_role`      varchar(30)  DEFAULT NULL COMMENT '用户角色（CUSTOMER_SERVICE:客服 TESTER:测试 DEVELOPER:开发 SYSTEM:系统）',
    `action`         varchar(30)  NOT NULL COMMENT '动作类型（CREATE:创建 ASSIGN:分派 READ:阅读 START_PROCESS:开始处理 TRANSFER:转派 ESCALATE:流转 RETURN:退回 COMPLETE:完成）',
    `from_status`    varchar(50)  DEFAULT NULL COMMENT '原状态',
    `to_status`      varchar(50)  DEFAULT NULL COMMENT '目标状态',
    `from_user_id`   bigint(20)   DEFAULT NULL COMMENT '来源用户ID',
    `to_user_id`     bigint(20)   DEFAULT NULL COMMENT '目标用户ID',
    `remark`         varchar(500) DEFAULT NULL COMMENT '备注',
    `is_first_read`  tinyint(4)   DEFAULT NULL COMMENT '是否为该节点的首次阅读（0:否 1:是）',
    `timestamp`      datetime     NOT NULL COMMENT '事件发生时间',
    `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`      varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`        tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_time_track_ticket` (`ticket_id`, `timestamp`),
    KEY `idx_time_track_user` (`user_id`, `action`, `timestamp`),
    KEY `idx_action` (`action`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单时间追踪记录表';

-- -----------------------------------------------------------
-- 工单节点耗时统计汇总表
-- -----------------------------------------------------------
CREATE TABLE `ticket_node_duration` (
    `id`                   bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`            bigint(20)   NOT NULL COMMENT '工单ID',
    `node_name`            varchar(50)  NOT NULL COMMENT '节点名称（如：待测试受理/测试中/待开发受理/开发中/待验收/待客服确认）',
    `assignee_id`          bigint(20)   DEFAULT NULL COMMENT '处理人ID',
    `assignee_role`        varchar(30)  DEFAULT NULL COMMENT '处理人角色',
    `arrive_at`            datetime     DEFAULT NULL COMMENT '到达节点时间',
    `first_read_at`        datetime     DEFAULT NULL COMMENT '首次阅读时间',
    `start_process_at`     datetime     DEFAULT NULL COMMENT '开始处理时间',
    `leave_at`             datetime     DEFAULT NULL COMMENT '离开节点时间',
    `wait_duration_sec`    bigint(20)   DEFAULT NULL COMMENT '等待耗时（秒）= 首次阅读时间 - 到达时间',
    `process_duration_sec` bigint(20)   DEFAULT NULL COMMENT '处理耗时（秒）= 离开时间 - 首次阅读时间',
    `total_duration_sec`   bigint(20)   DEFAULT NULL COMMENT '总耗时（秒）= 离开时间 - 到达时间',
    `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`            varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`            varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`              tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_node_name` (`node_name`),
    KEY `idx_assignee_id` (`assignee_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='工单节点耗时统计汇总表';
