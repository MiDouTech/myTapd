-- ============================================================
-- V59__init_weekly_invalid_report_config.sql
-- 无效反馈周报推送配置初始化
-- ============================================================

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'weekly_invalid_report_enabled', 'false', 'WEEKLY_INVALID_REPORT', '无效反馈周报自动推送开关', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `system_config`
    WHERE `config_key` = 'weekly_invalid_report_enabled'
      AND `config_group` = 'WEEKLY_INVALID_REPORT'
      AND `deleted` = 0
);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'weekly_invalid_report_cron', '0 0 18 ? * FRI', 'WEEKLY_INVALID_REPORT', '无效反馈周报推送cron', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `system_config`
    WHERE `config_key` = 'weekly_invalid_report_cron'
      AND `config_group` = 'WEEKLY_INVALID_REPORT'
      AND `deleted` = 0
);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'weekly_invalid_report_webhook_urls', '', 'WEEKLY_INVALID_REPORT', '无效反馈周报企微群Webhook地址（逗号分隔）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `system_config`
    WHERE `config_key` = 'weekly_invalid_report_webhook_urls'
      AND `config_group` = 'WEEKLY_INVALID_REPORT'
      AND `deleted` = 0
);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'weekly_invalid_report_stat_category_ids', '', 'WEEKLY_INVALID_REPORT', '无效反馈周报统计分类ID（逗号分隔）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `system_config`
    WHERE `config_key` = 'weekly_invalid_report_stat_category_ids'
      AND `config_group` = 'WEEKLY_INVALID_REPORT'
      AND `deleted` = 0
);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'weekly_invalid_report_max_detail_count', '30', 'WEEKLY_INVALID_REPORT', '无效反馈周报明细最大展示条数', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `system_config`
    WHERE `config_key` = 'weekly_invalid_report_max_detail_count'
      AND `config_group` = 'WEEKLY_INVALID_REPORT'
      AND `deleted` = 0
);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'weekly_invalid_report_timezone', 'Asia/Shanghai', 'WEEKLY_INVALID_REPORT', '无效反馈周报调度时区', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM `system_config`
    WHERE `config_key` = 'weekly_invalid_report_timezone'
      AND `config_group` = 'WEEKLY_INVALID_REPORT'
      AND `deleted` = 0
);
