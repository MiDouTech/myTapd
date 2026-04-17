-- ============================================================
-- V35__init_daily_report_config.sql
-- 日报自动推送功能：system_config 追加日报相关配置
-- 说明：部分环境可能已通过脚本/手工预置了同名配置键，需避免 uk_config_key 冲突导致迁移失败
-- ============================================================

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'daily_report_enabled', 'true', 'DAILY_REPORT', '日报自动推送开关（true:开启 false:关闭）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'daily_report_enabled');

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'daily_report_cron', '0 0 18 * * ?', 'DAILY_REPORT', '日报推送Cron表达式（默认每天18:00）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'daily_report_cron');

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'daily_report_webhook_urls', '', 'DAILY_REPORT', '日报推送企微群Webhook地址（多个用逗号分隔）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'daily_report_webhook_urls');

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'daily_report_include_defect_detail', 'true', 'DAILY_REPORT', '日报是否包含缺陷明细分类（true:包含 false:不包含）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'daily_report_include_defect_detail');

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'daily_report_include_suspended', 'true', 'DAILY_REPORT', '日报是否包含挂起工单列表（true:包含 false:不包含）', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'daily_report_include_suspended');
