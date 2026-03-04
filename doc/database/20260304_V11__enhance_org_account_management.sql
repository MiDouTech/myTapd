-- ============================================================
-- 20260304_V11__enhance_org_account_management.sql
-- 组织账号管理增强：补充员工性别字段与查询索引
-- ============================================================

-- -----------------------------------------------------------
-- 用户表增强：新增性别字段
-- -----------------------------------------------------------
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND COLUMN_NAME = 'gender'
    ),
    'SELECT 1',
    'ALTER TABLE `sys_user` ADD COLUMN `gender` tinyint(4) NOT NULL DEFAULT 0 COMMENT ''性别（0:未知 1:男 2:女）'' AFTER `position`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------
-- 用户表增强：补充组织查询常用组合索引
-- -----------------------------------------------------------
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'sys_user'
          AND INDEX_NAME = 'idx_department_status_deleted'
    ),
    'SELECT 1',
    'ALTER TABLE `sys_user` ADD KEY `idx_department_status_deleted` (`department_id`, `account_status`, `deleted`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
