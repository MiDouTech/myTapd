-- ============================================================
-- V36__init_basic_settings_config.sql
-- 基础参数配置初始化（BASIC 分组）
-- ============================================================

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'system_name', '米多工单系统', 'BASIC', '系统名称', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'system_name' AND `deleted` = 0);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'timezone', 'Asia/Shanghai', 'BASIC', '默认时区', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'timezone' AND `deleted` = 0);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'default_page_size', '20', 'BASIC', '默认分页条数', 'system', 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'default_page_size' AND `deleted` = 0);
