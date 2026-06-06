-- V61：工单视图分类过滤配置初始化
-- 用于将通用工单、缺陷工单、告警工单从列表口径上拆分；分类按环境真实ID配置，不按名称写死。

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'ticket_defect_category_ids', '', 'TICKET_VIEW', '缺陷工单分类ID列表（逗号分隔；为空时缺陷工单视图返回空）', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_config` WHERE `config_key` = 'ticket_defect_category_ids'
);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'ticket_alert_category_ids', '', 'TICKET_VIEW', '告警工单分类ID列表（逗号分隔；告警视图同时按source=alert查询）', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_config` WHERE `config_key` = 'ticket_alert_category_ids'
);

INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`, `create_by`, `update_by`)
SELECT 'ticket_general_exclude_alert_source', 'true', 'TICKET_VIEW', '通用工单是否排除source=alert的告警工单', 'system', 'system'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `system_config` WHERE `config_key` = 'ticket_general_exclude_alert_source'
);
