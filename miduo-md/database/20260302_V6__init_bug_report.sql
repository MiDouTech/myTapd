-- ============================================================
-- V6__init_bug_report.sql
-- Bug简报管理表：简报主表、责任人、工单关联、日志、附件、字典表
-- ============================================================

-- -----------------------------------------------------------
-- Bug简报主表
-- -----------------------------------------------------------
CREATE TABLE `bug_report` (
    `id`                  bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_no`           varchar(50)  NOT NULL COMMENT '简报编号（如BR-20260228-015）',
    `status`              varchar(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '简报状态（DRAFT:待填写 PENDING_REVIEW:待审核 REJECTED:已退回 ARCHIVED:已归档 VOIDED:已作废）',
    `problem_desc`        text         DEFAULT NULL COMMENT '问题描述',
    `logic_cause_level1`  varchar(50)  DEFAULT NULL COMMENT '逻辑归因一级分类',
    `logic_cause_level2`  varchar(50)  DEFAULT NULL COMMENT '逻辑归因二级分类',
    `logic_cause_detail`  text         DEFAULT NULL COMMENT '归因详细说明',
    `defect_category`     varchar(50)  DEFAULT NULL COMMENT '缺陷分类',
    `introduced_project`  varchar(200) DEFAULT NULL COMMENT '引入项目',
    `start_date`          date         DEFAULT NULL COMMENT '开始时间（缺陷发现日期）',
    `resolve_date`        date         DEFAULT NULL COMMENT '解决时间（修复上线日期）',
    `solution`            text         DEFAULT NULL COMMENT '解决方案',
    `impact_scope`        text         DEFAULT NULL COMMENT '影响范围',
    `severity_level`      varchar(10)  DEFAULT NULL COMMENT '缺陷等级（P0:致命 P1:严重 P2:一般 P3:轻微 P4:建议）',
    `reporter_id`         bigint(20)   DEFAULT NULL COMMENT '反馈人ID',
    `reviewer_id`         bigint(20)   DEFAULT NULL COMMENT '审核人ID',
    `remark`              text         DEFAULT NULL COMMENT '备注',
    `submitted_at`        datetime     DEFAULT NULL COMMENT '提交审核时间',
    `reviewed_at`         datetime     DEFAULT NULL COMMENT '审核时间',
    `review_comment`      text         DEFAULT NULL COMMENT '审核意见',
    `created_by_user_id`  bigint(20)   DEFAULT NULL COMMENT '创建人用户ID',
    `create_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`           varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`           varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`             tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_no` (`report_no`),
    KEY `idx_bug_report_status` (`status`, `create_time`),
    KEY `idx_severity_level` (`severity_level`),
    KEY `idx_reporter_id` (`reporter_id`),
    KEY `idx_reviewer_id` (`reviewer_id`),
    KEY `idx_defect_category` (`defect_category`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Bug简报主表';

-- -----------------------------------------------------------
-- 简报责任人关联表（多对多）
-- -----------------------------------------------------------
CREATE TABLE `bug_report_responsible` (
    `id`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`   bigint(20)  NOT NULL COMMENT '简报ID',
    `user_id`     bigint(20)  NOT NULL COMMENT '责任人用户ID',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_user` (`report_id`, `user_id`),
    KEY `idx_report_id` (`report_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='简报责任人关联表';

-- -----------------------------------------------------------
-- 简报与工单关联表（多对多）
-- -----------------------------------------------------------
CREATE TABLE `bug_report_ticket` (
    `id`              bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`       bigint(20)  NOT NULL COMMENT '简报ID',
    `ticket_id`       bigint(20)  NOT NULL COMMENT '工单ID',
    `is_auto_created` tinyint(4)  NOT NULL DEFAULT 0 COMMENT '是否自动创建关联（0:手动 1:自动）',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       varchar(50) NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`       varchar(50) NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`         tinyint(4)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_ticket` (`report_id`, `ticket_id`),
    KEY `idx_report_id` (`report_id`),
    KEY `idx_bug_report_ticket` (`ticket_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='简报与工单关联表';

-- -----------------------------------------------------------
-- 简报操作日志表
-- -----------------------------------------------------------
CREATE TABLE `bug_report_log` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`   bigint(20)   NOT NULL COMMENT '简报ID',
    `user_id`     bigint(20)   NOT NULL COMMENT '操作用户ID',
    `action`      varchar(30)  NOT NULL COMMENT '操作类型（CREATE:创建 SUBMIT:提交审核 APPROVE:审核通过 REJECT:审核不通过 VOID:作废 EDIT:编辑）',
    `old_status`  varchar(20)  DEFAULT NULL COMMENT '变更前状态',
    `new_status`  varchar(20)  DEFAULT NULL COMMENT '变更后状态',
    `remark`      text         DEFAULT NULL COMMENT '操作备注/审核意见',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_report_id` (`report_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_action` (`action`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='简报操作日志表';

-- -----------------------------------------------------------
-- 简报附件表
-- -----------------------------------------------------------
CREATE TABLE `bug_report_attachment` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`   bigint(20)   NOT NULL COMMENT '简报ID',
    `file_name`   varchar(255) NOT NULL COMMENT '文件名',
    `file_path`   varchar(500) NOT NULL COMMENT '文件存储路径',
    `file_size`   bigint(20)   NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `uploaded_by`  bigint(20)  NOT NULL COMMENT '上传人ID',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_report_id` (`report_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='简报附件表';

-- -----------------------------------------------------------
-- 逻辑归因字典表（二级级联）
-- -----------------------------------------------------------
CREATE TABLE `dict_logic_cause` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `level`       tinyint(4)   NOT NULL COMMENT '层级（1:一级归因 2:二级归因）',
    `name`        varchar(100) NOT NULL COMMENT '归因名称',
    `parent_id`   bigint(20)   DEFAULT NULL COMMENT '父级ID（二级归因关联一级）',
    `sort_order`  int(11)      NOT NULL DEFAULT 0 COMMENT '排序号',
    `is_active`   tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_level` (`level`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='逻辑归因字典表';

-- -----------------------------------------------------------
-- 缺陷分类字典表
-- -----------------------------------------------------------
CREATE TABLE `dict_defect_category` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(100) NOT NULL COMMENT '分类名称',
    `description` varchar(500) DEFAULT NULL COMMENT '分类描述',
    `sort_order`  int(11)      NOT NULL DEFAULT 0 COMMENT '排序号',
    `is_active`   tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='缺陷分类字典表';

-- -----------------------------------------------------------
-- 项目字典表
-- -----------------------------------------------------------
CREATE TABLE `dict_project` (
    `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(200) NOT NULL COMMENT '项目名称',
    `description` varchar(500) DEFAULT NULL COMMENT '项目描述',
    `is_active`   tinyint(4)   NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '创建人',
    `update_by`   varchar(50)  NOT NULL DEFAULT '' COMMENT '更新人',
    `deleted`     tinyint(4)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='项目字典表';

-- -----------------------------------------------------------
-- 初始化逻辑归因字典数据
-- -----------------------------------------------------------
INSERT INTO `dict_logic_cause` (`id`, `level`, `name`, `parent_id`, `sort_order`, `create_by`, `update_by`) VALUES
(1,  1, '配置错误',   NULL, 1, 'system', 'system'),
(2,  2, '参数配置错误', 1,   1, 'system', 'system'),
(3,  2, '环境配置错误', 1,   2, 'system', 'system'),
(4,  2, '权限配置错误', 1,   3, 'system', 'system'),
(5,  2, '开关配置遗漏', 1,   4, 'system', 'system'),
(6,  1, '编码缺陷',   NULL, 2, 'system', 'system'),
(7,  2, '逻辑错误',   6,    1, 'system', 'system'),
(8,  2, '空指针/异常处理', 6, 2, 'system', 'system'),
(9,  2, '边界条件遗漏', 6,   3, 'system', 'system'),
(10, 2, '并发问题',   6,    4, 'system', 'system'),
(11, 2, '内存泄漏',   6,    5, 'system', 'system'),
(12, 1, '设计缺陷',   NULL, 3, 'system', 'system'),
(13, 2, '需求理解偏差', 12,  1, 'system', 'system'),
(14, 2, '架构设计不合理', 12, 2, 'system', 'system'),
(15, 2, '接口设计不当', 12,  3, 'system', 'system'),
(16, 2, '兼容性考虑不足', 12, 4, 'system', 'system'),
(17, 1, '第三方问题',  NULL, 4, 'system', 'system'),
(18, 2, '第三方SDK缺陷', 17, 1, 'system', 'system'),
(19, 2, '第三方接口变更', 17, 2, 'system', 'system'),
(20, 2, '第三方服务不稳定', 17, 3, 'system', 'system'),
(21, 1, '数据问题',   NULL, 5, 'system', 'system'),
(22, 2, '脏数据',     21,   1, 'system', 'system'),
(23, 2, '数据迁移错误', 21,  2, 'system', 'system'),
(24, 2, '数据库设计缺陷', 21, 3, 'system', 'system'),
(25, 2, '缓存一致性问题', 21, 4, 'system', 'system'),
(26, 1, '运维问题',   NULL, 6, 'system', 'system'),
(27, 2, '部署操作失误', 26,  1, 'system', 'system'),
(28, 2, '资源不足',   26,   2, 'system', 'system'),
(29, 2, '网络故障',   26,   3, 'system', 'system'),
(30, 2, '证书/域名过期', 26, 4, 'system', 'system'),
(31, 1, '测试遗漏',   NULL, 7, 'system', 'system'),
(32, 2, '测试用例覆盖不足', 31, 1, 'system', 'system'),
(33, 2, '回归测试遗漏', 31,  2, 'system', 'system'),
(34, 2, '特定环境未测试', 31, 3, 'system', 'system');

-- -----------------------------------------------------------
-- 初始化缺陷分类字典数据
-- -----------------------------------------------------------
INSERT INTO `dict_defect_category` (`id`, `name`, `description`, `sort_order`, `create_by`, `update_by`) VALUES
(1, '功能异常', '功能逻辑不符合预期，操作无响应或结果错误', 1, 'system', 'system'),
(2, '交互异常', '界面展示异常、控件行为不符合预期、页面空白/错位', 2, 'system', 'system'),
(3, '性能问题', '响应缓慢、卡顿、超时、资源占用异常', 3, 'system', 'system'),
(4, '数据异常', '数据丢失、数据错误、数据不一致', 4, 'system', 'system'),
(5, '安全漏洞', '权限绕过、注入攻击、信息泄露', 5, 'system', 'system'),
(6, '兼容性问题', '浏览器/设备/系统版本兼容性问题', 6, 'system', 'system'),
(7, '接口异常', 'API返回异常、接口超时、参数校验失败', 7, 'system', 'system');
