-- V26：为分类表新增备注描述和自然语言匹配关键词字段
-- 部分环境已存在 `remark`（手工变更或历史漂移），直接 ADD 会导致 Flyway 失败、服务 502；
-- 这里按列存在性分别执行，保证迁移幂等。
SET @v26_db = DATABASE();

SET @v26_sql = (
    SELECT IF(
        (SELECT COUNT(*)
         FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @v26_db
           AND TABLE_NAME = 'ticket_category'
           AND COLUMN_NAME = 'remark') > 0,
        'SELECT 1',
        'ALTER TABLE `ticket_category` ADD COLUMN `remark` varchar(500) DEFAULT NULL COMMENT ''分类备注描述，说明该分类通常涵盖的问题范围'' AFTER `is_active`'
    )
);
PREPARE v26_stmt FROM @v26_sql;
EXECUTE v26_stmt;
DEALLOCATE PREPARE v26_stmt;

SET @v26_sql = (
    SELECT IF(
        (SELECT COUNT(*)
         FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @v26_db
           AND TABLE_NAME = 'ticket_category'
           AND COLUMN_NAME = 'nl_match_keywords') > 0,
        'SELECT 1',
        'ALTER TABLE `ticket_category` ADD COLUMN `nl_match_keywords` text DEFAULT NULL COMMENT ''自然语言匹配关键词，逗号分隔，用于企微消息自动归类'' AFTER `remark`'
    )
);
PREPARE v26_stmt FROM @v26_sql;
EXECUTE v26_stmt;
DEALLOCATE PREPARE v26_stmt;
