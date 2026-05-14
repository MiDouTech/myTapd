-- V55：日报统计分类ID配置项初始化
-- 支持按分类ID过滤日报统计口径，避免不同环境分类名称差异导致统计错误。
INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'daily_report_stat_category_ids', '', 'DAILY_REPORT', '日报统计分类ID列表（逗号分隔，空表示全量）', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_config` WHERE `config_key` = 'daily_report_stat_category_ids'
);
