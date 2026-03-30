/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80035 (8.0.35)
 Source Host           : localhost:3306
 Source Schema         : ticket_platform

 Target Server Type    : MySQL
 Target Server Version : 80035 (8.0.35)
 File Encoding         : 65001

 Date: 05/03/2026 14:20:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bug_report
-- ----------------------------
DROP TABLE IF EXISTS `bug_report`;
CREATE TABLE `bug_report`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '简报编号（如BR-20260228-015）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '简报状态（DRAFT:待填写 PENDING_REVIEW:待审核 REJECTED:已退回 ARCHIVED:已归档 VOIDED:已作废）',
  `problem_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '问题描述',
  `logic_cause_level1` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '逻辑归因一级分类',
  `logic_cause_level2` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '逻辑归因二级分类',
  `logic_cause_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '归因详细说明',
  `defect_category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缺陷分类',
  `introduced_project` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '引入项目',
  `start_date` date NULL DEFAULT NULL COMMENT '开始时间（缺陷发现日期）',
  `resolve_date` date NULL DEFAULT NULL COMMENT '解决时间（修复上线日期）',
  `solution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '解决方案',
  `impact_scope` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '影响范围',
  `severity_level` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缺陷等级（P0:致命 P1:严重 P2:一般 P3:轻微 P4:建议）',
  `reporter_id` bigint NULL DEFAULT NULL COMMENT '反馈人ID',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人ID',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注',
  `submitted_at` datetime NULL DEFAULT NULL COMMENT '提交审核时间',
  `reviewed_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核意见',
  `created_by_user_id` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_report_no`(`report_no` ASC) USING BTREE,
  INDEX `idx_bug_report_status`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_severity_level`(`severity_level` ASC) USING BTREE,
  INDEX `idx_reporter_id`(`reporter_id` ASC) USING BTREE,
  INDEX `idx_reviewer_id`(`reviewer_id` ASC) USING BTREE,
  INDEX `idx_defect_category`(`defect_category` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Bug简报主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bug_report
-- ----------------------------
INSERT INTO `bug_report` VALUES (1, 'BR-20260303-003-3955', 'DRAFT', 'trest', '设计缺陷', '兼容性考虑不足', NULL, '数据异常', NULL, '2026-03-03', '2026-03-03', NULL, NULL, 'P2', 10001, NULL, NULL, NULL, NULL, NULL, 10001, '2026-03-03 23:48:45', '2026-03-03 23:48:45', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for bug_report_attachment
-- ----------------------------
DROP TABLE IF EXISTS `bug_report_attachment`;
CREATE TABLE `bug_report_attachment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '简报ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储路径',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `uploaded_by` bigint NOT NULL COMMENT '上传人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report_id`(`report_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '简报附件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bug_report_attachment
-- ----------------------------

-- ----------------------------
-- Table structure for bug_report_log
-- ----------------------------
DROP TABLE IF EXISTS `bug_report_log`;
CREATE TABLE `bug_report_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '简报ID',
  `user_id` bigint NOT NULL COMMENT '操作用户ID',
  `action` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型（CREATE:创建 SUBMIT:提交审核 APPROVE:审核通过 REJECT:审核不通过 VOID:作废 EDIT:编辑）',
  `old_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更前状态',
  `new_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更后状态',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '操作备注/审核意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report_id`(`report_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_action`(`action` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '简报操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bug_report_log
-- ----------------------------
INSERT INTO `bug_report_log` VALUES (1, 1, 10001, 'CREATE', NULL, 'DRAFT', '创建Bug简报', '2026-03-03 23:48:45', '2026-03-03 23:48:45', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for bug_report_responsible
-- ----------------------------
DROP TABLE IF EXISTS `bug_report_responsible`;
CREATE TABLE `bug_report_responsible`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '简报ID',
  `user_id` bigint NOT NULL COMMENT '责任人用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_report_user`(`report_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_report_id`(`report_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '简报责任人关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bug_report_responsible
-- ----------------------------
INSERT INTO `bug_report_responsible` VALUES (1, 1, 10001, '2026-03-03 23:48:45', '2026-03-03 23:48:45', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for bug_report_ticket
-- ----------------------------
DROP TABLE IF EXISTS `bug_report_ticket`;
CREATE TABLE `bug_report_ticket`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '简报ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `is_auto_created` tinyint NOT NULL DEFAULT 0 COMMENT '是否自动创建关联（0:手动 1:自动）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_report_ticket`(`report_id` ASC, `ticket_id` ASC) USING BTREE,
  INDEX `idx_report_id`(`report_id` ASC) USING BTREE,
  INDEX `idx_bug_report_ticket`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '简报与工单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bug_report_ticket
-- ----------------------------
INSERT INTO `bug_report_ticket` VALUES (1, 1, 19, 0, '2026-03-03 23:48:45', '2026-03-03 23:48:45', 'debug-user', 'debug-user', 0);
INSERT INTO `bug_report_ticket` VALUES (2, 1, 18, 0, '2026-03-03 23:48:45', '2026-03-03 23:48:45', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父部门ID',
  `wecom_dept_id` bigint NULL DEFAULT NULL COMMENT '企微部门ID',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `dept_status` tinyint NOT NULL DEFAULT 1 COMMENT '部门状态（1:启用 0:停用）',
  `sync_status` tinyint NOT NULL DEFAULT 0 COMMENT '同步状态（0:未同步 1:成功 2:失败）',
  `sync_time` datetime NULL DEFAULT NULL COMMENT '最近同步时间',
  `leader_wecom_userid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '部门负责人企微UserID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_department_wecom_dept_id`(`wecom_dept_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_wecom_dept_id`(`wecom_dept_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of department
-- ----------------------------

-- ----------------------------
-- Table structure for dict_defect_category
-- ----------------------------
DROP TABLE IF EXISTS `dict_defect_category`;
CREATE TABLE `dict_defect_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类描述',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '缺陷分类字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dict_defect_category
-- ----------------------------
INSERT INTO `dict_defect_category` VALUES (1, '功能异常', '功能逻辑不符合预期，操作无响应或结果错误', 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_defect_category` VALUES (2, '交互异常', '界面展示异常、控件行为不符合预期、页面空白/错位', 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_defect_category` VALUES (3, '性能问题', '响应缓慢、卡顿、超时、资源占用异常', 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_defect_category` VALUES (4, '数据异常', '数据丢失、数据错误、数据不一致', 4, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_defect_category` VALUES (5, '安全漏洞', '权限绕过、注入攻击、信息泄露', 5, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_defect_category` VALUES (6, '兼容性问题', '浏览器/设备/系统版本兼容性问题', 6, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_defect_category` VALUES (7, '接口异常', 'API返回异常、接口超时、参数校验失败', 7, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);

-- ----------------------------
-- Table structure for dict_logic_cause
-- ----------------------------
DROP TABLE IF EXISTS `dict_logic_cause`;
CREATE TABLE `dict_logic_cause`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `level` tinyint NOT NULL COMMENT '层级（1:一级归因 2:二级归因）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '归因名称',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父级ID（二级归因关联一级）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '逻辑归因字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dict_logic_cause
-- ----------------------------
INSERT INTO `dict_logic_cause` VALUES (1, 1, '配置错误', NULL, 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (2, 2, '参数配置错误', 1, 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (3, 2, '环境配置错误', 1, 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (4, 2, '权限配置错误', 1, 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (5, 2, '开关配置遗漏', 1, 4, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (6, 1, '编码缺陷', NULL, 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (7, 2, '逻辑错误', 6, 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (8, 2, '空指针/异常处理', 6, 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (9, 2, '边界条件遗漏', 6, 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (10, 2, '并发问题', 6, 4, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (11, 2, '内存泄漏', 6, 5, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (12, 1, '设计缺陷', NULL, 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (13, 2, '需求理解偏差', 12, 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (14, 2, '架构设计不合理', 12, 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (15, 2, '接口设计不当', 12, 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (16, 2, '兼容性考虑不足', 12, 4, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (17, 1, '第三方问题', NULL, 4, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (18, 2, '第三方SDK缺陷', 17, 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (19, 2, '第三方接口变更', 17, 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (20, 2, '第三方服务不稳定', 17, 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (21, 1, '数据问题', NULL, 5, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (22, 2, '脏数据', 21, 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (23, 2, '数据迁移错误', 21, 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (24, 2, '数据库设计缺陷', 21, 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (25, 2, '缓存一致性问题', 21, 4, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (26, 1, '运维问题', NULL, 6, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (27, 2, '部署操作失误', 26, 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (28, 2, '资源不足', 26, 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (29, 2, '网络故障', 26, 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (30, 2, '证书/域名过期', 26, 4, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (31, 1, '测试遗漏', NULL, 7, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (32, 2, '测试用例覆盖不足', 31, 1, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (33, 2, '回归测试遗漏', 31, 2, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);
INSERT INTO `dict_logic_cause` VALUES (34, 2, '特定环境未测试', 31, 3, 1, '2026-03-02 20:16:06', '2026-03-02 20:16:06', 'system', 'system', 0);

-- ----------------------------
-- Table structure for dict_project
-- ----------------------------
DROP TABLE IF EXISTS `dict_project`;
CREATE TABLE `dict_project`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '项目名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '项目描述',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '项目字典表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dict_project
-- ----------------------------

-- ----------------------------
-- Table structure for dispatch_rule
-- ----------------------------
DROP TABLE IF EXISTS `dispatch_rule`;
CREATE TABLE `dispatch_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则名称',
  `category_id` bigint NULL DEFAULT NULL COMMENT '关联分类ID',
  `strategy` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MANUAL' COMMENT '分派策略（MANUAL:手动 CATEGORY_DEFAULT:分类默认 ROUND_ROBIN:轮询 LOAD_BALANCE:负载均衡 MATRIX:矩阵分派）',
  `target_group_id` bigint NULL DEFAULT NULL COMMENT '目标处理组ID',
  `target_user_id` bigint NULL DEFAULT NULL COMMENT '目标处理人ID',
  `rule_config` json NULL COMMENT '规则配置（JSON格式，矩阵分派时存储条件匹配规则）',
  `priority_order` int NOT NULL DEFAULT 0 COMMENT '规则优先级（数字越小优先级越高）',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_strategy`(`strategy` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分派规则表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dispatch_rule
-- ----------------------------

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of flyway_schema_history
-- ----------------------------
INSERT INTO `flyway_schema_history` VALUES (1, '1', 'init base', 'SQL', 'V1__init_base.sql', 414827089, 'root', '2026-03-02 20:15:48', 2991, 1);
INSERT INTO `flyway_schema_history` VALUES (2, '2', 'init ticket core', 'SQL', 'V2__init_ticket_core.sql', -894440400, 'root', '2026-03-02 20:15:54', 6031, 1);
INSERT INTO `flyway_schema_history` VALUES (3, '3', 'init workflow sla', 'SQL', 'V3__init_workflow_sla.sql', -1851898150, 'root', '2026-03-02 20:15:58', 3615, 1);
INSERT INTO `flyway_schema_history` VALUES (4, '4', 'init time track', 'SQL', 'V4__init_time_track.sql', -269819133, 'root', '2026-03-02 20:16:00', 1487, 1);
INSERT INTO `flyway_schema_history` VALUES (5, '5', 'init bug ticket', 'SQL', 'V5__init_bug_ticket.sql', 1616309216, 'root', '2026-03-02 20:16:01', 1515, 1);
INSERT INTO `flyway_schema_history` VALUES (6, '6', 'init bug report', 'SQL', 'V6__init_bug_report.sql', -1840395314, 'root', '2026-03-02 20:16:06', 4950, 1);
INSERT INTO `flyway_schema_history` VALUES (7, '7', 'init wecom notification', 'SQL', 'V7__init_wecom_notification.sql', 1137033334, 'root', '2026-03-02 20:16:10', 3425, 1);
INSERT INTO `flyway_schema_history` VALUES (8, '8', 'alter bug ticket add screenshot', 'SQL', 'V8__alter_bug_ticket_add_screenshot.sql', 2040443368, 'root', '2026-03-03 09:42:13', 659, 1);
INSERT INTO `flyway_schema_history` VALUES (9, '9', 'init task010 webhook', 'SQL', 'V9__init_task010_webhook.sql', -657395263, 'root', '2026-03-03 09:42:15', 1542, 1);
INSERT INTO `flyway_schema_history` VALUES (10, '10', 'init wework identity reuse', 'SQL', 'V10__init_wework_identity_reuse.sql', -1838980378, 'root', '2026-03-04 15:20:39', 7892, 1);
INSERT INTO `flyway_schema_history` VALUES (11, '11', 'init webhook dispatch log', 'SQL', 'V11__init_webhook_dispatch_log.sql', -134921209, 'root', '2026-03-04 19:01:02', 1167, 1);

-- ----------------------------
-- Table structure for handler_group
-- ----------------------------
DROP TABLE IF EXISTS `handler_group`;
CREATE TABLE `handler_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '处理组名称',
  `leader_id` bigint NULL DEFAULT NULL COMMENT '组长用户ID',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理组描述',
  `skill_tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '技能标签（逗号分隔）',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_leader_id`(`leader_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处理组表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of handler_group
-- ----------------------------
INSERT INTO `handler_group` VALUES (1, 'test', 10001, 'test', 'test', 1, '2026-03-03 14:35:23', '2026-03-03 14:35:23', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for handler_group_member
-- ----------------------------
DROP TABLE IF EXISTS `handler_group_member`;
CREATE TABLE `handler_group_member`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` bigint NOT NULL COMMENT '处理组ID',
  `user_id` bigint NOT NULL COMMENT '成员用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_user`(`group_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '处理组成员关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of handler_group_member
-- ----------------------------
INSERT INTO `handler_group_member` VALUES (1, 1, 10001, '2026-03-03 14:35:23', '2026-03-03 14:35:23', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for notification
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '目标用户ID',
  `ticket_id` bigint NULL DEFAULT NULL COMMENT '关联工单ID',
  `report_id` bigint NULL DEFAULT NULL COMMENT '关联简报ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型（TICKET_CREATED/STATUS_CHANGED/ASSIGNED/SLA_WARNING/SLA_BREACHED/COMMENT/URGE/REPORT_REMIND等）',
  `channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SITE' COMMENT '渠道（SITE:站内信 WECOM_APP:企微应用消息 WECOM_GROUP:企微群 EMAIL:邮件）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读（0:未读 1:已读）',
  `read_at` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_read`(`user_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_channel`(`channel` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification
-- ----------------------------
INSERT INTO `notification` VALUES (1, 10001, 25, NULL, 'STATUS_CHANGED', 'SITE', '工单状态更新 - WO-20260304-002-9775', '工单编号：WO-20260304-002-9775\n标题：344\n状态：待受理 → 已关闭\n操作人：debug-user', 0, NULL, '2026-03-04 22:23:52', '2026-03-04 22:23:52', 'system', 'system', 0);
INSERT INTO `notification` VALUES (2, 10001, 29, NULL, 'ASSIGNED', 'SITE', '您有新的工单分派 - WO-20260304-001-4502', '工单编号：WO-20260304-001-4502\n标题：顺德\n分派人：debug-user\n优先级：medium', 0, NULL, '2026-03-04 22:24:49', '2026-03-04 22:24:49', 'system', 'system', 0);

-- ----------------------------
-- Table structure for notification_preference
-- ----------------------------
DROP TABLE IF EXISTS `notification_preference`;
CREATE TABLE `notification_preference`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型（与notification.type对应）',
  `site_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '站内信开关（0:关闭 1:开启）',
  `wecom_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '企微消息开关（0:关闭 1:开启）',
  `email_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '邮件开关（0:关闭 1:开启）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_event`(`user_id` ASC, `event_type` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户通知偏好表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification_preference
-- ----------------------------

-- ----------------------------
-- Table structure for sla_policy
-- ----------------------------
DROP TABLE IF EXISTS `sla_policy`;
CREATE TABLE `sla_policy`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '策略名称',
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '适用优先级（URGENT/HIGH/MEDIUM/LOW）',
  `response_time` int NOT NULL COMMENT '首次响应时限（分钟）',
  `resolve_time` int NOT NULL COMMENT '解决时限（分钟）',
  `warning_pct` int NOT NULL DEFAULT 75 COMMENT '预警百分比阈值',
  `critical_pct` int NOT NULL DEFAULT 90 COMMENT '告警百分比阈值',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '策略描述',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_priority`(`priority` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SLA策略表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sla_policy
-- ----------------------------
INSERT INTO `sla_policy` VALUES (1, '紧急SLA策略', 'URGENT', 15, 120, 75, 90, '生产系统故障、全公司影响：15分钟响应，2小时解决', 1, '2026-03-02 20:15:58', '2026-03-02 20:15:58', 'system', 'system', 0);
INSERT INTO `sla_policy` VALUES (2, '高SLA策略', 'HIGH', 30, 240, 75, 90, '部门级影响、核心功能故障：30分钟响应，4小时解决', 1, '2026-03-02 20:15:58', '2026-03-02 20:15:58', 'system', 'system', 0);
INSERT INTO `sla_policy` VALUES (3, '中SLA策略', 'MEDIUM', 120, 480, 75, 90, '个人工作受影响：2小时响应，8小时（1工作日）解决', 1, '2026-03-02 20:15:58', '2026-03-02 20:15:58', 'system', 'system', 0);
INSERT INTO `sla_policy` VALUES (4, '低SLA策略', 'LOW', 240, 1440, 75, 90, '咨询类、优化建议：4小时响应，24小时（3工作日）解决', 1, '2026-03-02 20:15:58', '2026-03-02 20:15:58', 'system', 'system', 0);

-- ----------------------------
-- Table structure for sla_timer
-- ----------------------------
DROP TABLE IF EXISTS `sla_timer`;
CREATE TABLE `sla_timer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `sla_policy_id` bigint NOT NULL COMMENT 'SLA策略ID',
  `timer_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计时器类型（RESPONSE:响应 RESOLVE:解决）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'RUNNING' COMMENT '状态（RUNNING:运行中 PAUSED:已暂停 COMPLETED:已完成 BREACHED:已超时）',
  `threshold_minutes` int NOT NULL COMMENT '时限（分钟）',
  `elapsed_minutes` int NOT NULL DEFAULT 0 COMMENT '已消耗工作时间（分钟）',
  `start_at` datetime NOT NULL COMMENT '计时开始时间',
  `pause_at` datetime NULL DEFAULT NULL COMMENT '暂停时间',
  `deadline` datetime NULL DEFAULT NULL COMMENT '截止时间（预计算的工作时间截止点）',
  `breached_at` datetime NULL DEFAULT NULL COMMENT '超时时间',
  `completed_at` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `is_warned` tinyint NOT NULL DEFAULT 0 COMMENT '是否已预警（0:否 1:是）',
  `is_breached` tinyint NOT NULL DEFAULT 0 COMMENT '是否已超时（0:否 1:是）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_timer_type_status`(`timer_type` ASC, `status` ASC) USING BTREE,
  INDEX `idx_deadline`(`deadline` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'SLA计时器表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sla_timer
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `role_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_code`(`role_code` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'ADMIN', '系统管理员', '全部权限：系统配置、分类管理、工作流管理、用户管理', '2026-03-02 20:15:48', '2026-03-02 20:15:48', 'system', 'system', 0);
INSERT INTO `sys_role` VALUES (2, 'TICKET_ADMIN', '工单管理员', '所有工单的查看、分派、转派、关闭', '2026-03-02 20:15:48', '2026-03-02 20:15:48', 'system', 'system', 0);
INSERT INTO `sys_role` VALUES (3, 'HANDLER', '处理人', '处理分配给自己的工单，查看相关工单', '2026-03-02 20:15:48', '2026-03-02 20:15:48', 'system', 'system', 0);
INSERT INTO `sys_role` VALUES (4, 'SUBMITTER', '提交人', '创建工单、查看自己的工单、验收、催办', '2026-03-02 20:15:48', '2026-03-02 20:15:48', 'system', 'system', 0);
INSERT INTO `sys_role` VALUES (5, 'OBSERVER', '观察者', '仅查看权限，查看范围受部门限制', '2026-03-02 20:15:48', '2026-03-02 20:15:48', 'system', 'system', 0);

-- ----------------------------
-- Table structure for sys_sync_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_sync_log`;
CREATE TABLE `sys_sync_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sync_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '同步类型（DEPARTMENT/EMPLOYEE/FULL）',
  `sync_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '同步模式（MANUAL/SCHEDULE）',
  `sync_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '同步状态（SUCCESS/FAILED/PARTIAL）',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总处理数',
  `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '耗时（毫秒）',
  `trigger_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '触发人（定时任务可为空）',
  `error_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误码',
  `error_message` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误原因',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sync_type`(`sync_type` ASC) USING BTREE,
  INDEX `idx_sync_mode`(`sync_mode` ASC) USING BTREE,
  INDEX `idx_sync_status`(`sync_status` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企微同步日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_sync_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '姓名',
  `employee_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工号',
  `department_id` bigint NULL DEFAULT NULL COMMENT '所属部门ID',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `position` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职位',
  `gender` tinyint NOT NULL DEFAULT 0 COMMENT '性别（0:未知 1:男 2:女）',
  `avatar_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `wecom_userid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企微用户标识',
  `account_status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态（1:已激活 2:已禁用 4:未激活）',
  `sync_status` tinyint NOT NULL DEFAULT 0 COMMENT '同步状态（0:未同步 1:成功 2:失败）',
  `sync_time` datetime NULL DEFAULT NULL COMMENT '最近同步时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wecom_userid`(`wecom_userid` ASC) USING BTREE,
  INDEX `idx_department_id`(`department_id` ASC) USING BTREE,
  INDEX `idx_employee_no`(`employee_no` ASC) USING BTREE,
  INDEX `idx_account_status`(`account_status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE,
  INDEX `idx_department_status_deleted`(`department_id` ASC, `account_status` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10002 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (10001, 'debug-user', 'DEBUG001', NULL, 'debug@local', '13800000000', '本地调试', 0, NULL, 'debug_local_10001', 1, 0, NULL, '2026-03-02 22:03:40', '2026-03-02 22:03:40', 'system', 'system', 0);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 10001, 1, '2026-03-02 22:03:40', '2026-03-02 22:03:40', 'system', 'system', 0);

-- ----------------------------
-- Table structure for sys_wework_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_wework_config`;
CREATE TABLE `sys_wework_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `corp_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '企业微信CorpID',
  `agent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '企业微信AgentID',
  `corp_secret` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '企业微信应用Secret（密文）',
  `api_base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'https://qyapi.weixin.qq.com' COMMENT '企微API基础地址',
  `connect_timeout_ms` int NOT NULL DEFAULT 10000 COMMENT '连接超时（毫秒）',
  `read_timeout_ms` int NOT NULL DEFAULT 30000 COMMENT '读取超时（毫秒）',
  `schedule_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否开启定时同步（0:否 1:是）',
  `schedule_cron` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '定时同步Cron表达式',
  `retry_count` int NOT NULL DEFAULT 3 COMMENT '失败重试次数',
  `batch_size` int NOT NULL DEFAULT 100 COMMENT '同步批次大小',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '配置状态（1:启用 0:停用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_corp_agent`(`corp_id` ASC, `agent_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企业微信配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_wework_config
-- ----------------------------
-- 企微配置请在部署后通过管理后台或直接 INSERT 填入真实的 corp_id / agent_id / secret
-- INSERT INTO `sys_wework_config` VALUES (1, '<YOUR_CORP_ID>', '<YOUR_AGENT_ID>', '<YOUR_ENCRYPTED_SECRET>', 'https://qyapi.weixin.qq.com', 10000, 30000, 0, NULL, 0, 100, 1, NOW(), NOW(), 'admin', 'admin', 0);

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
  `config_group` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '配置分组（DEFAULT/WORKING_TIME/HOLIDAY/TICKET/WECOM/SLA）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE,
  INDEX `idx_config_group`(`config_group` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_config
-- ----------------------------
INSERT INTO `system_config` VALUES (1, 'working_time_start', '09:00', 'WORKING_TIME', '工作时间开始（HH:mm）', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (2, 'working_time_end', '18:00', 'WORKING_TIME', '工作时间结束（HH:mm）', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (3, 'working_days', '1,2,3,4,5', 'WORKING_TIME', '工作日（1=周一 ... 7=周日，逗号分隔）', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (4, 'ticket_no_prefix', 'WO', 'TICKET', '通用工单编号前缀', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (5, 'bug_ticket_no_prefix', 'BUG', 'TICKET', '缺陷工单编号前缀', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (6, 'bug_report_no_prefix', 'BR', 'TICKET', 'Bug简报编号前缀', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (7, 'attachment_max_size_mb', '20', 'TICKET', '附件最大文件大小（MB）', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (8, 'auto_close_days', '7', 'TICKET', '待验收状态超过N天自动关闭', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (9, 'bug_report_remind_days', '3', 'TICKET', '工单关闭后N天未填写简报自动催促', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (10, 'notification_aggregate_minutes', '5', 'DEFAULT', '同一工单N分钟内的多次变更合并为一条通知', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (11, 'wecom_corp_id', '', 'WECOM', '企业微信CorpID', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (12, 'wecom_agent_id', '', 'WECOM', '企微自建应用AgentID', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (13, 'wecom_secret', '', 'WECOM', '企微应用Secret', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (14, 'wecom_callback_token', '', 'WECOM', '企微回调Token', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);
INSERT INTO `system_config` VALUES (15, 'wecom_callback_aes_key', '', 'WECOM', '企微回调EncodingAESKey', '2026-03-02 20:16:10', '2026-03-02 20:16:10', 'system', 'system', 0);

-- ----------------------------
-- Table structure for ticket
-- ----------------------------
DROP TABLE IF EXISTS `ticket`;
CREATE TABLE `ticket`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单编号（业务可读，如WO-20260228-001）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '工单描述（富文本）',
  `category_id` bigint NULL DEFAULT NULL COMMENT '工单分类ID',
  `template_id` bigint NULL DEFAULT NULL COMMENT '工单模板ID',
  `workflow_id` bigint NULL DEFAULT NULL COMMENT '关联工作流ID',
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级（URGENT:紧急 HIGH:高 MEDIUM:中 LOW:低）',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '工单状态',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `creator_wework_userid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人企微UserID',
  `assignee_id` bigint NULL DEFAULT NULL COMMENT '当前处理人ID',
  `assignee_wework_userid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理人企微UserID',
  `current_dept_id` bigint NULL DEFAULT NULL COMMENT '当前处理部门ID',
  `source` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'WEB' COMMENT '来源（WEB:网页 WECOM_BOT:企微群机器人 API:接口）',
  `source_chat_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源企微群ID（企微群创建时记录）',
  `custom_fields` json NULL COMMENT '自定义字段值（JSON格式）',
  `expected_time` datetime NULL DEFAULT NULL COMMENT '期望完成时间',
  `resolved_at` datetime NULL DEFAULT NULL COMMENT '解决时间',
  `closed_at` datetime NULL DEFAULT NULL COMMENT '关闭时间',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_no`(`ticket_no` ASC) USING BTREE,
  INDEX `idx_ticket_assignee_status`(`assignee_id` ASC, `status` ASC, `priority` ASC) USING BTREE,
  INDEX `idx_ticket_creator_status`(`creator_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_ticket_category_status`(`category_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_ticket_created_at`(`create_time` DESC) USING BTREE,
  INDEX `idx_ticket_updated_at`(`update_time` DESC) USING BTREE,
  INDEX `idx_ticket_sla`(`status` ASC, `priority` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_ticket_source`(`source` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE,
  INDEX `idx_ticket_creator_wework_userid`(`creator_wework_userid` ASC) USING BTREE,
  INDEX `idx_ticket_assignee_wework_userid`(`assignee_wework_userid` ASC) USING BTREE,
  INDEX `idx_ticket_current_dept_id`(`current_dept_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket
-- ----------------------------
INSERT INTO `ticket` VALUES (18, 'WO-20260303-001-7595', 'test', '', 1, 1, 1, 'medium', 'PENDING', 10001, NULL, 10001, NULL, NULL, 'web', NULL, NULL, NULL, NULL, NULL, 1, '2026-03-03 22:05:09', '2026-03-03 22:05:09', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (19, 'WO-20260303-002-9106', 'test2', '', 1, 1, 1, 'medium', 'CLOSED', 10001, NULL, 10001, NULL, NULL, 'web', NULL, NULL, NULL, NULL, '2026-03-03 23:47:18', 1, '2026-03-03 23:46:52', '2026-03-03 23:46:52', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (20, 'WO-20260303-004-8978', 'sd', '', 1, 1, 1, 'medium', 'CLOSED', 10001, NULL, 10001, NULL, NULL, 'web', NULL, NULL, NULL, NULL, '2026-03-03 23:52:27', 3, '2026-03-03 23:50:02', '2026-03-03 23:50:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (21, 'WO-20260304-006-9767', '亡羊补牢', '', 1, 1, 1, 'medium', 'CLOSED', 10001, NULL, 10001, NULL, NULL, 'wecom', NULL, NULL, '2026-03-04 11:19:54', NULL, '2026-03-04 15:42:04', 2, '2026-03-04 11:20:10', '2026-03-04 11:20:10', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (22, 'WO-20260304-001-7330', 'ee', '', 1, 1, 1, 'medium', 'PENDING', 10001, NULL, 10001, NULL, NULL, 'web', NULL, NULL, NULL, NULL, NULL, 0, '2026-03-04 16:19:46', '2026-03-04 16:19:46', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (23, 'WO-20260304-001-6751', 'tesggg', '', 1, 1, 1, 'medium', 'PENDING', 10001, NULL, NULL, NULL, NULL, 'web', NULL, NULL, NULL, NULL, NULL, 0, '2026-03-04 16:31:33', '2026-03-04 16:31:33', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (24, 'WO-20260304-001-1583', 'testwewew', '', 1, 1, 1, 'medium', 'PENDING', 10001, NULL, NULL, NULL, NULL, 'web', NULL, NULL, NULL, NULL, NULL, 0, '2026-03-04 17:31:07', '2026-03-04 17:31:07', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (25, 'WO-20260304-002-9775', '344', '43', 1, 1, 1, 'medium', 'CLOSED', 10001, NULL, NULL, NULL, NULL, 'web', NULL, NULL, '2026-03-04 00:00:00', NULL, '2026-03-04 22:23:52', 1, '2026-03-04 18:25:02', '2026-03-04 18:25:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (26, 'WO-20260304-003-8081', '233', '', 1, 1, 1, 'medium', 'CLOSED', 10001, NULL, NULL, NULL, NULL, 'web', NULL, NULL, NULL, NULL, '2026-03-04 18:52:20', 1, '2026-03-04 18:51:02', '2026-03-04 18:51:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (27, 'WO-20260304-001-5152', '2323', '', 1, 1, 1, 'medium', 'CLOSED', 10001, NULL, NULL, NULL, NULL, 'web', NULL, NULL, NULL, NULL, '2026-03-04 20:02:09', 1, '2026-03-04 19:01:22', '2026-03-04 19:01:22', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (28, 'WO-20260304-002-1233', '订单', '', 1, 1, 1, 'medium', 'CLOSED', 10001, NULL, NULL, NULL, NULL, 'web', NULL, NULL, NULL, NULL, '2026-03-04 20:01:33', 1, '2026-03-04 19:07:51', '2026-03-04 19:07:51', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket` VALUES (29, 'WO-20260304-001-4502', '顺德', '是多少', 1, 1, 1, 'medium', 'CLOSED', 10001, NULL, 10001, NULL, NULL, 'web', NULL, NULL, NULL, NULL, '2026-03-04 19:36:06', 3, '2026-03-04 19:33:50', '2026-03-04 19:33:50', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for ticket_assignment_rule
-- ----------------------------
DROP TABLE IF EXISTS `ticket_assignment_rule`;
CREATE TABLE `ticket_assignment_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_category_id` bigint NULL DEFAULT NULL COMMENT '工单分类ID',
  `priority` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '优先级（URGENT/HIGH/MEDIUM/LOW）',
  `target_dept_id` bigint NULL DEFAULT NULL COMMENT '目标处理部门ID',
  `target_role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标角色编码',
  `default_assignee_wework_userid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认处理人企微UserID',
  `fallback_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ADMIN' COMMENT '兜底类型（ADMIN/QUEUE/NONE）',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1:启用 0:停用）',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '优先级排序（越小越优先）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_priority`(`ticket_category_id` ASC, `priority` ASC) USING BTREE,
  INDEX `idx_target_dept`(`target_dept_id` ASC) USING BTREE,
  INDEX `idx_target_role`(`target_role_code` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单自动派单规则表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_assignment_rule
-- ----------------------------

-- ----------------------------
-- Table structure for ticket_attachment
-- ----------------------------
DROP TABLE IF EXISTS `ticket_attachment`;
CREATE TABLE `ticket_attachment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储路径',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件MIME类型',
  `uploaded_by` bigint NOT NULL COMMENT '上传人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单附件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_attachment
-- ----------------------------

-- ----------------------------
-- Table structure for ticket_bug_dev_info
-- ----------------------------
DROP TABLE IF EXISTS `ticket_bug_dev_info`;
CREATE TABLE `ticket_bug_dev_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `root_cause` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '缺陷原因',
  `fix_solution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '修复方案',
  `git_branch` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联分支/提交',
  `impact_assessment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '影响范围评估',
  `dev_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '开发备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '缺陷工单开发信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_bug_dev_info
-- ----------------------------

-- ----------------------------
-- Table structure for ticket_bug_info
-- ----------------------------
DROP TABLE IF EXISTS `ticket_bug_info`;
CREATE TABLE `ticket_bug_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `merchant_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户编号',
  `company_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `merchant_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商户账号',
  `problem_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '问题描述',
  `expected_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预期结果',
  `scene_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '场景码',
  `problem_screenshot` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '问题截图（URL或逗号分隔）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_merchant_no`(`merchant_no` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '缺陷工单客服信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_bug_info
-- ----------------------------
INSERT INTO `ticket_bug_info` VALUES (1, 18, 'test', 'test', 'te', 'e', 'e', 'e', '', '2026-03-03 23:32:32', '2026-03-03 23:32:32', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for ticket_bug_test_info
-- ----------------------------
DROP TABLE IF EXISTS `ticket_bug_test_info`;
CREATE TABLE `ticket_bug_test_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `reproduce_env` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '复现环境（PRODUCTION:生产环境 TEST:测试环境 BOTH:均可复现）',
  `reproduce_steps` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '复现步骤',
  `actual_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '实际结果',
  `impact_scope` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '影响范围（SINGLE:单一商户 PARTIAL:部分商户 ALL:全部商户）',
  `severity_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '缺陷等级（FATAL:致命 CRITICAL:严重 NORMAL:一般 MINOR:轻微）',
  `module_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属模块',
  `reproduce_screenshot` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '复现截图（URL或逗号分隔）',
  `test_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '测试备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_severity_level`(`severity_level` ASC) USING BTREE,
  INDEX `idx_module_name`(`module_name` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '缺陷工单测试信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_bug_test_info
-- ----------------------------
INSERT INTO `ticket_bug_test_info` VALUES (1, 20, 'PRODUCTION', 'd', 'd', 'SINGLE', 'CRITICAL', '', '', '', '2026-03-03 23:51:29', '2026-03-03 23:51:29', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for ticket_category
-- ----------------------------
DROP TABLE IF EXISTS `ticket_category`;
CREATE TABLE `ticket_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父分类ID',
  `level` tinyint NOT NULL DEFAULT 1 COMMENT '分类层级（1:一级 2:二级 3:三级）',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类全路径（如 /1/2/3/）',
  `template_id` bigint NULL DEFAULT NULL COMMENT '关联的工单模板ID',
  `workflow_id` bigint NULL DEFAULT NULL COMMENT '关联的工作流ID',
  `sla_policy_id` bigint NULL DEFAULT NULL COMMENT '关联的SLA策略ID',
  `default_group_id` bigint NULL DEFAULT NULL COMMENT '默认处理组ID',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_workflow_id`(`workflow_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_category
-- ----------------------------
INSERT INTO `ticket_category` VALUES (1, 'test', NULL, 1, '/1/', 1, 1, 1, 1, 0, 1, '2026-03-02 22:11:46', '2026-03-02 22:11:46', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for ticket_comment
-- ----------------------------
DROP TABLE IF EXISTS `ticket_comment`;
CREATE TABLE `ticket_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'COMMENT' COMMENT '类型（COMMENT:评论 OPERATION:操作记录）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单评论/处理记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_comment
-- ----------------------------

-- ----------------------------
-- Table structure for ticket_custom_field
-- ----------------------------
DROP TABLE IF EXISTS `ticket_custom_field`;
CREATE TABLE `ticket_custom_field`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `field_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字段键名',
  `field_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '字段值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ticket_field`(`ticket_id` ASC, `field_key` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单自定义字段值表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_custom_field
-- ----------------------------

-- ----------------------------
-- Table structure for ticket_follower
-- ----------------------------
DROP TABLE IF EXISTS `ticket_follower`;
CREATE TABLE `ticket_follower`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `user_id` bigint NOT NULL COMMENT '关注人ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_user`(`ticket_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单关注人表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_follower
-- ----------------------------
INSERT INTO `ticket_follower` VALUES (1, 18, 10001, '2026-03-03 23:31:51', '2026-03-03 23:31:51', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_follower` VALUES (2, 20, 10001, '2026-03-03 23:50:42', '2026-03-03 23:50:42', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for ticket_log
-- ----------------------------
DROP TABLE IF EXISTS `ticket_log`;
CREATE TABLE `ticket_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `user_id` bigint NOT NULL COMMENT '操作用户ID',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型（CREATE/UPDATE/ASSIGN/TRANSFER/CLOSE/REOPEN等）',
  `old_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '变更前的值',
  `new_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '变更后的值',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_action`(`action` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_log
-- ----------------------------
INSERT INTO `ticket_log` VALUES (18, 18, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260303-001-7595', '2026-03-03 22:05:09', '2026-03-03 22:05:09', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (19, 18, 10001, 'FOLLOW', NULL, NULL, NULL, '2026-03-03 23:31:51', '2026-03-03 23:31:51', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (20, 18, 10001, 'ASSIGN', '', '10001', '', '2026-03-03 23:33:49', '2026-03-03 23:33:49', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (21, 19, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260303-002-9106', '2026-03-03 23:46:52', '2026-03-03 23:46:52', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (22, 19, 10001, 'CLOSE', 'PENDING', 'CLOSED', 's', '2026-03-03 23:47:18', '2026-03-03 23:47:18', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (23, 20, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260303-004-8978', '2026-03-03 23:50:02', '2026-03-03 23:50:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (24, 20, 10001, 'ASSIGN', '10001', '10001', '', '2026-03-03 23:50:08', '2026-03-03 23:50:08', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (25, 20, 10001, 'FOLLOW', NULL, NULL, NULL, '2026-03-03 23:50:42', '2026-03-03 23:50:42', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (26, 20, 10001, 'ASSIGN', '10001', '10001', '', '2026-03-03 23:50:57', '2026-03-03 23:50:57', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (27, 20, 10001, 'TRANSIT', 'PENDING', 'CLOSED', '看板拖拽状态变更', '2026-03-03 23:52:27', '2026-03-03 23:52:27', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (28, 21, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-006-9767', '2026-03-04 11:20:10', '2026-03-04 11:20:10', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (29, 21, 10001, 'ASSIGN', '10001', '10001', '', '2026-03-04 15:42:01', '2026-03-04 15:42:01', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (30, 21, 10001, 'CLOSE', 'PENDING', 'CLOSED', '', '2026-03-04 15:42:04', '2026-03-04 15:42:04', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (31, 22, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-001-7330', '2026-03-04 16:19:46', '2026-03-04 16:19:46', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (32, 23, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-001-6751', '2026-03-04 16:31:33', '2026-03-04 16:31:33', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (33, 24, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-001-1583', '2026-03-04 17:31:07', '2026-03-04 17:31:07', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (34, 25, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-002-9775', '2026-03-04 18:25:02', '2026-03-04 18:25:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (35, 26, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-003-8081', '2026-03-04 18:51:02', '2026-03-04 18:51:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (36, 26, 10001, 'CLOSE', 'PENDING', 'CLOSED', '', '2026-03-04 18:52:20', '2026-03-04 18:52:20', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (37, 27, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-001-5152', '2026-03-04 19:01:22', '2026-03-04 19:01:22', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (38, 28, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-002-1233', '2026-03-04 19:07:51', '2026-03-04 19:07:51', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (39, 29, 10001, 'CREATE', NULL, 'PENDING', '创建工单: WO-20260304-001-4502', '2026-03-04 19:33:50', '2026-03-04 19:33:50', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (40, 29, 10001, 'ASSIGN', '10001', '10001', '', '2026-03-04 19:35:17', '2026-03-04 19:35:17', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (41, 29, 10001, 'CLOSE', 'PENDING', 'CLOSED', '关闭测试', '2026-03-04 19:36:06', '2026-03-04 19:36:06', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (42, 28, 10001, 'TRANSIT', 'PENDING', 'CLOSED', '看板拖拽状态变更', '2026-03-04 20:01:33', '2026-03-04 20:01:33', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (43, 27, 10001, 'TRANSIT', 'PENDING', 'CLOSED', '看板拖拽状态变更', '2026-03-04 20:02:09', '2026-03-04 20:02:09', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (44, 25, 10001, 'TRANSIT', 'PENDING', 'CLOSED', '看板拖拽状态变更', '2026-03-04 22:23:52', '2026-03-04 22:23:52', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_log` VALUES (45, 29, 10001, 'ASSIGN', '10001', '10001', 'dd', '2026-03-04 22:24:49', '2026-03-04 22:24:49', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for ticket_node_duration
-- ----------------------------
DROP TABLE IF EXISTS `ticket_node_duration`;
CREATE TABLE `ticket_node_duration`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `node_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '节点名称（如：待测试受理/测试中/待开发受理/开发中/待验收/待客服确认）',
  `assignee_id` bigint NULL DEFAULT NULL COMMENT '处理人ID',
  `assignee_role` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理人角色',
  `arrive_at` datetime NULL DEFAULT NULL COMMENT '到达节点时间',
  `first_read_at` datetime NULL DEFAULT NULL COMMENT '首次阅读时间',
  `start_process_at` datetime NULL DEFAULT NULL COMMENT '开始处理时间',
  `leave_at` datetime NULL DEFAULT NULL COMMENT '离开节点时间',
  `wait_duration_sec` bigint NULL DEFAULT NULL COMMENT '等待耗时（秒）= 首次阅读时间 - 到达时间',
  `process_duration_sec` bigint NULL DEFAULT NULL COMMENT '处理耗时（秒）= 离开时间 - 首次阅读时间',
  `total_duration_sec` bigint NULL DEFAULT NULL COMMENT '总耗时（秒）= 离开时间 - 到达时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_node_name`(`node_name` ASC) USING BTREE,
  INDEX `idx_assignee_id`(`assignee_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单节点耗时统计汇总表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_node_duration
-- ----------------------------
INSERT INTO `ticket_node_duration` VALUES (1, 18, 'PENDING', 10001, 'SYSTEM', '2026-03-03 22:05:09', '2026-03-03 22:05:10', NULL, NULL, 0, NULL, NULL, '2026-03-03 22:05:09', '2026-03-03 22:05:09', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (2, 19, 'PENDING', 10001, 'SYSTEM', '2026-03-03 23:46:52', '2026-03-03 23:46:52', NULL, '2026-03-03 23:47:18', 0, 25, 25, '2026-03-03 23:46:52', '2026-03-03 23:46:52', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (3, 19, 'CLOSED', 10001, 'SYSTEM', '2026-03-03 23:47:18', NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-03 23:47:18', '2026-03-03 23:47:18', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (4, 20, 'PENDING', 10001, 'SYSTEM', '2026-03-03 23:50:02', '2026-03-03 23:50:02', NULL, '2026-03-03 23:52:27', 0, 144, 144, '2026-03-03 23:50:02', '2026-03-03 23:50:02', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (5, 20, 'CLOSED', 10001, 'SYSTEM', '2026-03-03 23:52:27', NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-03 23:52:27', '2026-03-03 23:52:27', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (6, 21, 'PENDING', 10001, 'SYSTEM', '2026-03-04 11:20:10', '2026-03-04 11:20:11', NULL, '2026-03-04 15:42:04', 1, 15713, 15714, '2026-03-04 11:20:10', '2026-03-04 11:20:10', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (7, 21, 'CLOSED', 10001, 'SYSTEM', '2026-03-04 15:42:04', NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-04 15:42:04', '2026-03-04 15:42:04', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (8, 22, 'PENDING', 10001, 'SYSTEM', '2026-03-04 16:19:46', '2026-03-04 16:19:47', NULL, NULL, 0, NULL, NULL, '2026-03-04 16:19:46', '2026-03-04 16:19:46', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (9, 23, 'PENDING', NULL, NULL, '2026-03-04 16:31:33', '2026-03-04 16:31:34', NULL, NULL, 0, NULL, NULL, '2026-03-04 16:31:33', '2026-03-04 16:31:33', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (10, 24, 'PENDING', NULL, NULL, '2026-03-04 17:31:08', '2026-03-04 17:31:08', NULL, NULL, 0, NULL, NULL, '2026-03-04 17:31:08', '2026-03-04 17:31:08', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (11, 25, 'PENDING', NULL, NULL, '2026-03-04 18:25:02', '2026-03-04 18:25:02', NULL, '2026-03-04 22:23:52', 0, 14330, 14330, '2026-03-04 18:25:02', '2026-03-04 18:25:02', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (12, 26, 'PENDING', NULL, NULL, '2026-03-04 18:51:02', '2026-03-04 18:51:02', NULL, '2026-03-04 18:52:20', 0, 77, 77, '2026-03-04 18:51:02', '2026-03-04 18:51:02', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (13, 26, 'CLOSED', NULL, NULL, '2026-03-04 18:52:20', NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-04 18:52:20', '2026-03-04 18:52:20', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (14, 27, 'PENDING', NULL, NULL, '2026-03-04 19:01:22', '2026-03-04 19:01:22', NULL, '2026-03-04 20:02:09', 0, 3646, 3646, '2026-03-04 19:01:22', '2026-03-04 19:01:22', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (15, 28, 'PENDING', NULL, NULL, '2026-03-04 19:07:51', '2026-03-04 19:07:52', NULL, '2026-03-04 20:01:33', 1, 3221, 3222, '2026-03-04 19:07:51', '2026-03-04 19:07:51', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (16, 29, 'PENDING', 10001, 'SYSTEM', '2026-03-04 19:33:50', '2026-03-04 19:33:51', NULL, '2026-03-04 19:36:06', 1, 134, 135, '2026-03-04 19:33:50', '2026-03-04 19:33:50', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (17, 29, 'CLOSED', 10001, 'SYSTEM', '2026-03-04 19:36:06', '2026-03-04 22:24:44', NULL, NULL, 10117, NULL, NULL, '2026-03-04 19:36:06', '2026-03-04 19:36:06', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (18, 28, 'CLOSED', NULL, NULL, '2026-03-04 20:01:33', NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-04 20:01:34', '2026-03-04 20:01:34', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (19, 27, 'CLOSED', NULL, NULL, '2026-03-04 20:02:09', NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-04 20:02:09', '2026-03-04 20:02:09', 'system', 'system', 0);
INSERT INTO `ticket_node_duration` VALUES (20, 25, 'CLOSED', NULL, NULL, '2026-03-04 22:23:52', NULL, NULL, NULL, NULL, NULL, NULL, '2026-03-04 22:23:52', '2026-03-04 22:23:52', 'system', 'system', 0);

-- ----------------------------
-- Table structure for ticket_template
-- ----------------------------
DROP TABLE IF EXISTS `ticket_template`;
CREATE TABLE `ticket_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `category_id` bigint NULL DEFAULT NULL COMMENT '关联分类ID',
  `fields_config` json NULL COMMENT '自定义字段配置（JSON格式）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模板描述',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_template
-- ----------------------------

-- ----------------------------
-- Table structure for ticket_time_track
-- ----------------------------
DROP TABLE IF EXISTS `ticket_time_track`;
CREATE TABLE `ticket_time_track`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ticket_id` bigint NOT NULL COMMENT '工单ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '操作用户ID',
  `user_role` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户角色（CUSTOMER_SERVICE:客服 TESTER:测试 DEVELOPER:开发 SYSTEM:系统）',
  `action` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '动作类型（CREATE:创建 ASSIGN:分派 READ:阅读 START_PROCESS:开始处理 TRANSFER:转派 ESCALATE:流转 RETURN:退回 COMPLETE:完成）',
  `from_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标状态',
  `from_user_id` bigint NULL DEFAULT NULL COMMENT '来源用户ID',
  `to_user_id` bigint NULL DEFAULT NULL COMMENT '目标用户ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `is_first_read` tinyint NULL DEFAULT NULL COMMENT '是否为该节点的首次阅读（0:否 1:是）',
  `timestamp` datetime NOT NULL COMMENT '事件发生时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_time_track_ticket`(`ticket_id` ASC, `timestamp` ASC) USING BTREE,
  INDEX `idx_time_track_user`(`user_id` ASC, `action` ASC, `timestamp` ASC) USING BTREE,
  INDEX `idx_action`(`action` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 67 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单时间追踪记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_time_track
-- ----------------------------
INSERT INTO `ticket_time_track` VALUES (18, 18, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, NULL, '创建工单: WO-20260303-001-7595', NULL, '2026-03-03 22:05:09', '2026-03-03 22:05:09', '2026-03-03 22:05:09', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (19, 18, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-03 22:05:10', '2026-03-03 22:05:10', '2026-03-03 22:05:10', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (20, 18, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-03 23:31:48', '2026-03-03 23:31:48', '2026-03-03 23:31:48', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (21, 18, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-03 23:32:51', '2026-03-03 23:32:51', '2026-03-03 23:32:51', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (22, 18, 10001, 'SYSTEM', 'ASSIGN', 'PENDING', 'PENDING', NULL, 10001, '', NULL, '2026-03-03 23:33:49', '2026-03-03 23:33:49', '2026-03-03 23:33:49', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (23, 19, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, 10001, '创建工单: WO-20260303-002-9106', NULL, '2026-03-03 23:46:52', '2026-03-03 23:46:52', '2026-03-03 23:46:52', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (24, 19, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-03 23:46:52', '2026-03-03 23:46:52', '2026-03-03 23:46:52', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (25, 19, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-03 23:47:14', '2026-03-03 23:47:14', '2026-03-03 23:47:14', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (26, 19, 10001, 'SYSTEM', 'COMPLETE', 'PENDING', 'CLOSED', 10001, 10001, 's', NULL, '2026-03-03 23:47:18', '2026-03-03 23:47:18', '2026-03-03 23:47:18', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (27, 20, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, 10001, '创建工单: WO-20260303-004-8978', NULL, '2026-03-03 23:50:02', '2026-03-03 23:50:02', '2026-03-03 23:50:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (28, 20, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-03 23:50:02', '2026-03-03 23:50:02', '2026-03-03 23:50:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (29, 20, 10001, 'SYSTEM', 'ASSIGN', 'PENDING', 'PENDING', 10001, 10001, '', NULL, '2026-03-03 23:50:08', '2026-03-03 23:50:08', '2026-03-03 23:50:08', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (30, 20, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-03 23:50:29', '2026-03-03 23:50:29', '2026-03-03 23:50:29', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (31, 20, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-03 23:50:51', '2026-03-03 23:50:51', '2026-03-03 23:50:51', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (32, 20, 10001, 'SYSTEM', 'ASSIGN', 'PENDING', 'PENDING', 10001, 10001, '', NULL, '2026-03-03 23:50:57', '2026-03-03 23:50:57', '2026-03-03 23:50:57', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (33, 20, 10001, 'SYSTEM', 'COMPLETE', 'PENDING', 'CLOSED', 10001, 10001, '看板拖拽状态变更', NULL, '2026-03-03 23:52:27', '2026-03-03 23:52:27', '2026-03-03 23:52:27', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (34, 21, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, 10001, '创建工单: WO-20260304-006-9767', NULL, '2026-03-04 11:20:10', '2026-03-04 11:20:10', '2026-03-04 11:20:10', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (35, 21, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 11:20:11', '2026-03-04 11:20:11', '2026-03-04 11:20:11', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (36, 21, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-04 11:25:53', '2026-03-04 11:25:53', '2026-03-04 11:25:53', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (37, 21, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-04 11:26:18', '2026-03-04 11:26:18', '2026-03-04 11:26:18', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (38, 21, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-04 15:41:45', '2026-03-04 15:41:45', '2026-03-04 15:41:45', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (39, 21, 10001, 'SYSTEM', 'ASSIGN', 'PENDING', 'PENDING', 10001, 10001, '', NULL, '2026-03-04 15:42:01', '2026-03-04 15:42:01', '2026-03-04 15:42:01', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (40, 21, 10001, 'SYSTEM', 'COMPLETE', 'PENDING', 'CLOSED', 10001, 10001, '', NULL, '2026-03-04 15:42:04', '2026-03-04 15:42:04', '2026-03-04 15:42:04', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (41, 22, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, 10001, '创建工单: WO-20260304-001-7330', NULL, '2026-03-04 16:19:46', '2026-03-04 16:19:46', '2026-03-04 16:19:46', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (42, 22, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 16:19:47', '2026-03-04 16:19:47', '2026-03-04 16:19:47', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (43, 23, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, NULL, '创建工单: WO-20260304-001-6751', NULL, '2026-03-04 16:31:33', '2026-03-04 16:31:33', '2026-03-04 16:31:33', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (44, 23, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 16:31:34', '2026-03-04 16:31:34', '2026-03-04 16:31:34', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (45, 24, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, NULL, '创建工单: WO-20260304-001-1583', NULL, '2026-03-04 17:31:08', '2026-03-04 17:31:08', '2026-03-04 17:31:08', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (46, 24, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 17:31:08', '2026-03-04 17:31:08', '2026-03-04 17:31:08', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (47, 25, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, NULL, '创建工单: WO-20260304-002-9775', NULL, '2026-03-04 18:25:02', '2026-03-04 18:25:02', '2026-03-04 18:25:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (48, 25, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 18:25:02', '2026-03-04 18:25:02', '2026-03-04 18:25:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (49, 26, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, NULL, '创建工单: WO-20260304-003-8081', NULL, '2026-03-04 18:51:02', '2026-03-04 18:51:02', '2026-03-04 18:51:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (50, 26, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 18:51:02', '2026-03-04 18:51:02', '2026-03-04 18:51:02', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (51, 26, 10001, 'SYSTEM', 'COMPLETE', 'PENDING', 'CLOSED', NULL, NULL, '', NULL, '2026-03-04 18:52:20', '2026-03-04 18:52:20', '2026-03-04 18:52:20', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (52, 27, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, NULL, '创建工单: WO-20260304-001-5152', NULL, '2026-03-04 19:01:22', '2026-03-04 19:01:22', '2026-03-04 19:01:22', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (53, 27, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 19:01:22', '2026-03-04 19:01:22', '2026-03-04 19:01:22', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (54, 27, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-04 19:06:35', '2026-03-04 19:06:35', '2026-03-04 19:06:35', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (55, 28, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, NULL, '创建工单: WO-20260304-002-1233', NULL, '2026-03-04 19:07:51', '2026-03-04 19:07:51', '2026-03-04 19:07:51', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (56, 28, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 19:07:52', '2026-03-04 19:07:52', '2026-03-04 19:07:52', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (57, 29, 10001, 'SYSTEM', 'CREATE', NULL, 'PENDING', NULL, 10001, '创建工单: WO-20260304-001-4502', NULL, '2026-03-04 19:33:50', '2026-03-04 19:33:50', '2026-03-04 19:33:50', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (58, 29, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 1, '2026-03-04 19:33:51', '2026-03-04 19:33:51', '2026-03-04 19:33:51', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (59, 29, 10001, 'SYSTEM', 'READ', 'PENDING', 'PENDING', NULL, 10001, NULL, 0, '2026-03-04 19:35:12', '2026-03-04 19:35:12', '2026-03-04 19:35:12', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (60, 29, 10001, 'SYSTEM', 'ASSIGN', 'PENDING', 'PENDING', 10001, 10001, '', NULL, '2026-03-04 19:35:17', '2026-03-04 19:35:17', '2026-03-04 19:35:17', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (61, 29, 10001, 'SYSTEM', 'COMPLETE', 'PENDING', 'CLOSED', 10001, 10001, '关闭测试', NULL, '2026-03-04 19:36:06', '2026-03-04 19:36:06', '2026-03-04 19:36:06', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (62, 28, 10001, 'SYSTEM', 'COMPLETE', 'PENDING', 'CLOSED', NULL, NULL, '看板拖拽状态变更', NULL, '2026-03-04 20:01:33', '2026-03-04 20:01:33', '2026-03-04 20:01:33', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (63, 27, 10001, 'SYSTEM', 'COMPLETE', 'PENDING', 'CLOSED', NULL, NULL, '看板拖拽状态变更', NULL, '2026-03-04 20:02:09', '2026-03-04 20:02:09', '2026-03-04 20:02:09', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (64, 25, 10001, 'SYSTEM', 'COMPLETE', 'PENDING', 'CLOSED', NULL, NULL, '看板拖拽状态变更', NULL, '2026-03-04 22:23:52', '2026-03-04 22:23:52', '2026-03-04 22:23:52', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (65, 29, 10001, 'SYSTEM', 'READ', 'CLOSED', 'CLOSED', NULL, 10001, NULL, 1, '2026-03-04 22:24:44', '2026-03-04 22:24:44', '2026-03-04 22:24:44', 'debug-user', 'debug-user', 0);
INSERT INTO `ticket_time_track` VALUES (66, 29, 10001, 'SYSTEM', 'ASSIGN', 'CLOSED', 'CLOSED', 10001, 10001, 'dd', NULL, '2026-03-04 22:24:49', '2026-03-04 22:24:49', '2026-03-04 22:24:49', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for ticket_user_role
-- ----------------------------
DROP TABLE IF EXISTS `ticket_user_role`;
CREATE TABLE `ticket_user_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `wework_userid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '企微UserID',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1:启用 0:停用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ticket_wework_role`(`wework_userid` ASC, `role_code` ASC) USING BTREE,
  INDEX `idx_wework_userid`(`wework_userid` ASC) USING BTREE,
  INDEX `idx_role_code`(`role_code` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单用户角色映射表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ticket_user_role
-- ----------------------------

-- ----------------------------
-- Table structure for webhook_config
-- ----------------------------
DROP TABLE IF EXISTS `webhook_config`;
CREATE TABLE `webhook_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Webhook回调地址',
  `secret` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '签名密钥',
  `event_types` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订阅事件类型（逗号分隔）',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `timeout_ms` int NOT NULL DEFAULT 5000 COMMENT '超时时间（毫秒）',
  `max_retry_times` int NOT NULL DEFAULT 0 COMMENT '失败重试次数',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置说明',
  `last_success_time` datetime NULL DEFAULT NULL COMMENT '最近成功推送时间',
  `last_fail_time` datetime NULL DEFAULT NULL COMMENT '最近失败推送时间',
  `last_fail_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_webhook_active`(`is_active` ASC) USING BTREE,
  INDEX `idx_webhook_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_webhook_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Webhook配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of webhook_config
-- ----------------------------
INSERT INTO `webhook_config` VALUES (1, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=1bbdfe4b-5c30-4d2c-a7fa-cca294eb2509', NULL, 'TICKET_CREATED,TICKET_STATUS_CHANGED,TICKET_ASSIGNED', 1, 5000, 3, NULL, '2026-03-04 15:42:05', NULL, NULL, '2026-03-04 15:41:35', '2026-03-04 16:18:35', 'debug-user', 'debug-user', 1);
INSERT INTO `webhook_config` VALUES (2, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=0a2ccc1e-fdce-430e-aaa1-eb79a13778f5', '0a2ccc1e-fdce-430e-aaa1-eb79a13778f5', 'TICKET_CREATED,TICKET_STATUS_CHANGED,TICKET_ASSIGNED', 1, 5000, 3, NULL, '2026-03-04 22:24:49', NULL, NULL, '2026-03-04 16:18:55', '2026-03-04 16:18:55', 'debug-user', 'debug-user', 0);

-- ----------------------------
-- Table structure for webhook_dispatch_log
-- ----------------------------
DROP TABLE IF EXISTS `webhook_dispatch_log`;
CREATE TABLE `webhook_dispatch_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `webhook_config_id` bigint NULL DEFAULT NULL COMMENT 'Webhook配置ID（未命中配置时为空）',
  `event_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '事件类型（TICKET_CREATED/TICKET_STATUS_CHANGED/TICKET_ASSIGNED）',
  `ticket_id` bigint NULL DEFAULT NULL COMMENT '工单ID',
  `request_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求地址（脱敏）',
  `request_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求体摘要',
  `attempt_no` int NOT NULL DEFAULT 0 COMMENT '当前尝试次数（从1开始，0表示未发起请求）',
  `max_attempts` int NOT NULL DEFAULT 0 COMMENT '最大尝试次数（含首次）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '推送状态（SUCCESS/FAIL/SKIPPED）',
  `response_code` int NULL DEFAULT NULL COMMENT 'HTTP响应码',
  `response_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '响应体摘要',
  `fail_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '失败原因',
  `duration_ms` bigint NULL DEFAULT NULL COMMENT '本次请求耗时（毫秒）',
  `dispatch_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分发时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_webhook_config_id`(`webhook_config_id` ASC) USING BTREE,
  INDEX `idx_event_ticket`(`event_type` ASC, `ticket_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_dispatch_time`(`dispatch_time` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Webhook推送明细日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of webhook_dispatch_log
-- ----------------------------
INSERT INTO `webhook_dispatch_log` VALUES (1, 2, 'TICKET_CREATED', 27, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send', '{\"data\":{\"categoryId\":1,\"priority\":\"medium\"},\"eventName\":\"工单创建\",\"eventTime\":\"2026-03-04 19:01:21\",\"eventType\":\"TICKET_CREATED\",\"ticket\":{\"creatorId\":10001,\"id\":27,\"priority\":\"medium\",\"status\":\"PENDING\",\"ticketNo\":\"WO-20260304-001-5152\",\"title\":\"2323\"},\"ticketId\":27}', 1, 4, 'SUCCESS', 200, '{\"errcode\":93000,\"errmsg\":\"invalid webhook url, hint: [1772622081038290197012894], from ip: 14.145.27.25, more info at https://open.work.weixin.qq.com/devtool/query?e=93000\"}', NULL, 248, '2026-03-04 19:01:22', '2026-03-04 19:01:22', '2026-03-04 19:01:22', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (2, 2, 'TICKET_CREATED', 28, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"data\":{\"categoryId\":1,\"priority\":\"medium\"},\"eventName\":\"工单创建\",\"eventTime\":\"2026-03-04 19:07:51\",\"eventType\":\"TICKET_CREATED\",\"ticket\":{\"creatorId\":10001,\"id\":28,\"priority\":\"medium\",\"status\":\"PENDING\",\"ticketNo\":\"WO-20260304-002-1233\",\"title\":\"订单\"},\"ticketId\":28}', 1, 4, 'SUCCESS', 200, '{\"errcode\":40008,\"errmsg\":\"invalid message type, hint: [1772622470401540439170671], from ip: 14.145.169.49, more info at https://open.work.weixin.qq.com/devtool/query?e=40008\"}', NULL, 155, '2026-03-04 19:07:52', '2026-03-04 19:07:52', '2026-03-04 19:07:52', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (3, 2, 'TICKET_ASSIGNED', 29, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"msgtype\":\"text\",\"text\":{\"content\":\"【工单事件通知】\\n事件：工单分派 (TICKET_ASSIGNED)\\n时间：2026-03-04 19:33:50\\n工单ID：29\\n工单编号：WO-20260304-001-4502\\n标题：顺德\\n状态：PENDING\\n优先级：medium\\n变更：{\\\"assignType\\\":\\\"CREATE_ASSIGN\\\",\\\"assigneeId\\\":10001,\\\"operatorId\\\":10001}\"}}', 1, 4, 'SUCCESS', 200, '{\"errcode\":0,\"errmsg\":\"ok\"}', NULL, 377, '2026-03-04 19:33:50', '2026-03-04 19:33:50', '2026-03-04 19:33:50', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (4, 2, 'TICKET_CREATED', 29, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"msgtype\":\"text\",\"text\":{\"content\":\"【工单事件通知】\\n事件：工单创建 (TICKET_CREATED)\\n时间：2026-03-04 19:33:50\\n工单ID：29\\n工单编号：WO-20260304-001-4502\\n标题：顺德\\n状态：PENDING\\n优先级：medium\\n变更：{\\\"categoryId\\\":1,\\\"priority\\\":\\\"medium\\\"}\"}}', 1, 4, 'SUCCESS', 200, '{\"errcode\":0,\"errmsg\":\"ok\"}', NULL, 393, '2026-03-04 19:33:51', '2026-03-04 19:33:51', '2026-03-04 19:33:51', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (5, 2, 'TICKET_ASSIGNED', 29, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"msgtype\":\"text\",\"text\":{\"content\":\"【工单事件通知】\\n事件：工单分派 (TICKET_ASSIGNED)\\n时间：2026-03-04 19:35:16\\n工单ID：29\\n工单编号：WO-20260304-001-4502\\n标题：顺德\\n状态：PENDING\\n优先级：medium\\n变更：{\\\"assignType\\\":\\\"MANUAL_ASSIGN\\\",\\\"assigneeId\\\":10001,\\\"operatorId\\\":10001,\\\"previousAssigneeId\\\":10001}\"}}', 1, 4, 'SUCCESS', 200, '{\"errcode\":0,\"errmsg\":\"ok\"}', NULL, 178, '2026-03-04 19:35:17', '2026-03-04 19:35:17', '2026-03-04 19:35:17', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (6, 2, 'TICKET_STATUS_CHANGED', 29, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"msgtype\":\"text\",\"text\":{\"content\":\"【工单事件通知】\\n事件：工单状态变更 (TICKET_STATUS_CHANGED)\\n时间：2026-03-04 19:36:06\\n工单ID：29\\n工单编号：WO-20260304-001-4502\\n标题：顺德\\n状态：CLOSED\\n优先级：medium\\n变更：{\\\"newStatus\\\":\\\"CLOSED\\\",\\\"oldStatus\\\":\\\"PENDING\\\",\\\"operatorId\\\":10001}\"}}', 1, 4, 'SUCCESS', 200, '{\"errcode\":0,\"errmsg\":\"ok\"}', NULL, 196, '2026-03-04 19:36:06', '2026-03-04 19:36:06', '2026-03-04 19:36:06', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (7, 2, 'TICKET_STATUS_CHANGED', 28, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"msgtype\":\"text\",\"text\":{\"content\":\"【工单事件通知】\\n事件：工单状态变更 (TICKET_STATUS_CHANGED)\\n时间：2026-03-04 20:01:33\\n工单ID：28\\n工单编号：WO-20260304-002-1233\\n标题：订单\\n状态：CLOSED\\n优先级：medium\\n变更：{\\\"newStatus\\\":\\\"CLOSED\\\",\\\"oldStatus\\\":\\\"PENDING\\\",\\\"operatorId\\\":10001}\"}}', 1, 4, 'SUCCESS', 200, '{\"errcode\":0,\"errmsg\":\"ok\"}', NULL, 220, '2026-03-04 20:01:34', '2026-03-04 20:01:34', '2026-03-04 20:01:34', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (8, 2, 'TICKET_STATUS_CHANGED', 27, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"msgtype\":\"text\",\"text\":{\"content\":\"【工单事件通知】\\n事件：工单状态变更 (TICKET_STATUS_CHANGED)\\n时间：2026-03-04 20:02:08\\n工单ID：27\\n工单编号：WO-20260304-001-5152\\n标题：2323\\n状态：CLOSED\\n优先级：medium\\n变更：{\\\"newStatus\\\":\\\"CLOSED\\\",\\\"oldStatus\\\":\\\"PENDING\\\",\\\"operatorId\\\":10001}\"}}', 1, 4, 'SUCCESS', 200, '{\"errcode\":0,\"errmsg\":\"ok\"}', NULL, 197, '2026-03-04 20:02:09', '2026-03-04 20:02:09', '2026-03-04 20:02:09', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (9, 2, 'TICKET_STATUS_CHANGED', 25, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"msgtype\":\"text\",\"text\":{\"content\":\"【工单事件通知】\\n事件：工单状态变更 (TICKET_STATUS_CHANGED)\\n时间：2026-03-04 22:23:52\\n工单ID：25\\n工单编号：WO-20260304-002-9775\\n标题：344\\n状态：CLOSED\\n优先级：medium\\n变更：{\\\"newStatus\\\":\\\"CLOSED\\\",\\\"oldStatus\\\":\\\"PENDING\\\",\\\"operatorId\\\":10001}\"}}', 1, 4, 'SUCCESS', 200, '{\"errcode\":0,\"errmsg\":\"ok\"}', NULL, 276, '2026-03-04 22:23:53', '2026-03-04 22:23:53', '2026-03-04 22:23:53', 'system', 'system', 0);
INSERT INTO `webhook_dispatch_log` VALUES (10, 2, 'TICKET_ASSIGNED', 29, 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?***', '{\"msgtype\":\"text\",\"text\":{\"content\":\"【工单事件通知】\\n事件：工单分派 (TICKET_ASSIGNED)\\n时间：2026-03-04 22:24:48\\n工单ID：29\\n工单编号：WO-20260304-001-4502\\n标题：顺德\\n状态：CLOSED\\n优先级：medium\\n变更：{\\\"assignType\\\":\\\"MANUAL_ASSIGN\\\",\\\"assigneeId\\\":10001,\\\"operatorId\\\":10001,\\\"previousAssigneeId\\\":10001}\"}}', 1, 4, 'SUCCESS', 200, '{\"errcode\":0,\"errmsg\":\"ok\"}', NULL, 180, '2026-03-04 22:24:49', '2026-03-04 22:24:49', '2026-03-04 22:24:49', 'system', 'system', 0);

-- ----------------------------
-- Table structure for wecom_bot_message_log
-- ----------------------------
DROP TABLE IF EXISTS `wecom_bot_message_log`;
CREATE TABLE `wecom_bot_message_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chat_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '来源群ChatID',
  `msg_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '企微消息ID（用于去重）',
  `from_wecom_userid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发送人企微UserID',
  `raw_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始消息内容',
  `parsed_result` json NULL COMMENT '解析结果（JSON格式）',
  `ticket_id` bigint NULL DEFAULT NULL COMMENT '创建的工单ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '处理状态（SUCCESS:成功 FAIL:失败 DUPLICATE:重复）',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chat_id`(`chat_id` ASC) USING BTREE,
  INDEX `idx_msg_id`(`msg_id` ASC) USING BTREE,
  INDEX `idx_from_wecom_userid`(`from_wecom_userid` ASC) USING BTREE,
  INDEX `idx_ticket_id`(`ticket_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企微机器人消息日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wecom_bot_message_log
-- ----------------------------

-- ----------------------------
-- Table structure for wecom_group_binding
-- ----------------------------
DROP TABLE IF EXISTS `wecom_group_binding`;
CREATE TABLE `wecom_group_binding`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chat_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '企微群ChatID',
  `chat_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群名称',
  `default_category_id` bigint NULL DEFAULT NULL COMMENT '默认工单分类ID',
  `webhook_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '群Webhook推送地址',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_chat_id`(`chat_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '企微群绑定配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wecom_group_binding
-- ----------------------------

-- ----------------------------
-- Table structure for workflow
-- ----------------------------
DROP TABLE IF EXISTS `workflow`;
CREATE TABLE `workflow`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作流名称',
  `mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'SIMPLE' COMMENT '模式（SIMPLE:简单模式 ADVANCED:高级模式）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作流描述',
  `states` json NOT NULL COMMENT '状态定义（JSON格式）',
  `transitions` json NOT NULL COMMENT '流转规则（JSON格式）',
  `is_builtin` tinyint NOT NULL DEFAULT 0 COMMENT '是否内置工作流（0:否 1:是）',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '启用状态（0:禁用 1:启用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标志（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mode`(`mode` ASC) USING BTREE,
  INDEX `idx_is_builtin`(`is_builtin` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作流定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workflow
-- ----------------------------
INSERT INTO `workflow` VALUES (1, '通用工单工作流', 'SIMPLE', '适用于一般工单的基础状态流转', '[{\"code\": \"PENDING\", \"name\": \"待受理\", \"type\": \"INITIAL\", \"slaAction\": \"START_RESPONSE\"}, {\"code\": \"PROCESSING\", \"name\": \"处理中\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESOLVE\"}, {\"code\": \"SUSPENDED\", \"name\": \"已挂起\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"PAUSE\"}, {\"code\": \"PENDING_VERIFY\", \"name\": \"待验收\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"PAUSE\"}, {\"code\": \"COMPLETED\", \"name\": \"已完成\", \"type\": \"TERMINAL\", \"slaAction\": \"STOP\"}, {\"code\": \"CLOSED\", \"name\": \"已关闭\", \"type\": \"TERMINAL\", \"slaAction\": \"STOP\"}]', '[{\"to\": \"PROCESSING\", \"from\": \"PENDING\", \"name\": \"受理\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}, {\"to\": \"SUSPENDED\", \"from\": \"PROCESSING\", \"name\": \"挂起\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}, {\"to\": \"PENDING_VERIFY\", \"from\": \"PROCESSING\", \"name\": \"处理完成\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}, {\"to\": \"PENDING\", \"from\": \"PROCESSING\", \"name\": \"转派\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}, {\"to\": \"PROCESSING\", \"from\": \"SUSPENDED\", \"name\": \"恢复处理\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}, {\"to\": \"COMPLETED\", \"from\": \"PENDING_VERIFY\", \"name\": \"验收通过\", \"allowedRoles\": [\"SUBMITTER\", \"ADMIN\"]}, {\"to\": \"PROCESSING\", \"from\": \"PENDING_VERIFY\", \"name\": \"验收不通过\", \"allowedRoles\": [\"SUBMITTER\", \"ADMIN\"]}, {\"to\": \"PENDING\", \"from\": \"COMPLETED\", \"name\": \"重新打开\", \"allowedRoles\": [\"SUBMITTER\", \"ADMIN\"]}, {\"to\": \"CLOSED\", \"from\": \"PENDING\", \"name\": \"关闭\", \"allowedRoles\": [\"SUBMITTER\", \"ADMIN\"]}]', 1, 1, '2026-03-02 20:15:58', '2026-03-02 20:15:58', 'system', 'system', 0);
INSERT INTO `workflow` VALUES (2, '审批工单工作流', 'ADVANCED', '适用于需要审批的工单类型', '[{\"code\": \"SUBMITTED\", \"name\": \"已提交\", \"type\": \"INITIAL\", \"slaAction\": \"START_RESPONSE\"}, {\"code\": \"DEPT_APPROVAL\", \"name\": \"部门审批\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESOLVE\"}, {\"code\": \"EXECUTING\", \"name\": \"执行中\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESOLVE\"}, {\"code\": \"COMPLETED\", \"name\": \"已完成\", \"type\": \"TERMINAL\", \"slaAction\": \"STOP\"}, {\"code\": \"REJECTED\", \"name\": \"已驳回\", \"type\": \"TERMINAL\", \"slaAction\": \"STOP\"}]', '[{\"to\": \"DEPT_APPROVAL\", \"from\": \"SUBMITTED\", \"name\": \"提交审批\", \"allowedRoles\": [\"SUBMITTER\"]}, {\"to\": \"EXECUTING\", \"from\": \"DEPT_APPROVAL\", \"name\": \"审批通过\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}, {\"to\": \"REJECTED\", \"from\": \"DEPT_APPROVAL\", \"name\": \"驳回\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}, {\"to\": \"SUBMITTED\", \"from\": \"REJECTED\", \"name\": \"修改重提\", \"allowedRoles\": [\"SUBMITTER\"]}, {\"to\": \"COMPLETED\", \"from\": \"EXECUTING\", \"name\": \"完成\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}]', 1, 1, '2026-03-02 20:15:58', '2026-03-02 20:15:58', 'system', 'system', 0);
INSERT INTO `workflow` VALUES (3, '缺陷工单工作流', 'ADVANCED', '缺陷工单专属流转，支持客服→测试→开发→验收→客服确认全链路', '[{\"code\": \"PENDING_DISPATCH\", \"name\": \"待分派\", \"type\": \"INITIAL\", \"slaAction\": \"START_RESPONSE\"}, {\"code\": \"PENDING_TEST\", \"name\": \"待测试受理\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESPONSE\"}, {\"code\": \"TESTING\", \"name\": \"测试中\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESOLVE\"}, {\"code\": \"PENDING_DEV\", \"name\": \"待开发受理\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESOLVE\"}, {\"code\": \"DEVELOPING\", \"name\": \"开发中\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESOLVE\"}, {\"code\": \"PENDING_VERIFY\", \"name\": \"待验收\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESOLVE\"}, {\"code\": \"PENDING_CS_CONFIRM\", \"name\": \"待客服确认\", \"type\": \"INTERMEDIATE\", \"slaAction\": \"START_RESOLVE\"}, {\"code\": \"COMPLETED\", \"name\": \"已完成\", \"type\": \"TERMINAL\", \"slaAction\": \"STOP\"}, {\"code\": \"CLOSED\", \"name\": \"已关闭\", \"type\": \"TERMINAL\", \"slaAction\": \"STOP\"}]', '[{\"to\": \"PENDING_TEST\", \"from\": \"PENDING_DISPATCH\", \"name\": \"分派测试\", \"allowedRoles\": [\"ADMIN\", \"TICKET_ADMIN\"]}, {\"to\": \"TESTING\", \"from\": \"PENDING_TEST\", \"name\": \"受理\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"PENDING_TEST\", \"from\": \"PENDING_TEST\", \"name\": \"转派测试\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"PENDING_DEV\", \"from\": \"TESTING\", \"name\": \"确认缺陷转开发\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"PENDING_TEST\", \"from\": \"TESTING\", \"name\": \"转派其他测试\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"CLOSED\", \"from\": \"TESTING\", \"name\": \"非缺陷关闭\", \"allowedRoles\": [\"HANDLER\", \"ADMIN\"]}, {\"to\": \"DEVELOPING\", \"from\": \"PENDING_DEV\", \"name\": \"受理\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"PENDING_DEV\", \"from\": \"PENDING_DEV\", \"name\": \"转派开发\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"PENDING_VERIFY\", \"from\": \"DEVELOPING\", \"name\": \"修复完成\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"PENDING_DEV\", \"from\": \"DEVELOPING\", \"name\": \"转派其他开发\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"PENDING_CS_CONFIRM\", \"from\": \"PENDING_VERIFY\", \"name\": \"验收通过\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"DEVELOPING\", \"from\": \"PENDING_VERIFY\", \"name\": \"验收不通过退回开发\", \"allowedRoles\": [\"HANDLER\"]}, {\"to\": \"COMPLETED\", \"from\": \"PENDING_CS_CONFIRM\", \"name\": \"客服确认关闭\", \"allowedRoles\": [\"HANDLER\", \"SUBMITTER\"]}, {\"to\": \"TESTING\", \"from\": \"PENDING_CS_CONFIRM\", \"name\": \"客户仍有问题退回测试\", \"allowedRoles\": [\"HANDLER\", \"SUBMITTER\"]}, {\"to\": \"CLOSED\", \"from\": \"PENDING_DISPATCH\", \"name\": \"直接关闭\", \"allowedRoles\": [\"ADMIN\"]}]', 1, 1, '2026-03-02 20:15:58', '2026-03-02 20:15:58', 'system', 'system', 0);

SET FOREIGN_KEY_CHECKS = 1;
